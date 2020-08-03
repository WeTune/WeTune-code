package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.ColumnAccessAnalyzer;
import sjtu.ipads.wtune.stmt.attrs.*;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.operators.*;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.BinaryOp.AND;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.Kind.SIMPLE;
import static sjtu.ipads.wtune.stmt.attrs.QueryScope.Clause.WHERE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;

public class ExposeDerivedTableSource implements RelationMutator {
  private final RelationGraph relationGraph;
  private final Relation target;
  private Set<JoinCondition> removedConds;
  private Set<JoinCondition> addedConds;

  public ExposeDerivedTableSource(final RelationGraph relationGraph, Relation target) {
    assert relationGraph != null && target != null;

    this.relationGraph = relationGraph;
    this.target = target;
  }

  public static boolean canExpose(SQLNode root, Relation target) {
    // exclude non table source
    if (!target.isTableSource()) return false;
    // exclude simple source
    if (target.node().get(TABLE_SOURCE_KIND) == SIMPLE) return false;
    // exclude UNION
    final SQLNode subquery = target.node().get(DERIVED_SUBQUERY).get(QUERY_BODY);
    if (subquery.type() != Type.QUERY_SPEC || subquery.get(QUERY_LIMIT) != null) return false;

    // now target must be a derived table source

    // condition:
    // the used column of this derived table source must be originated from a column ref expr.
    // i.e. it must have a simple name
    // e.g. SELECT 1 FROM (SELECT x AS x, y + 1 AS y FROM b) AS a WHERE a.y = 3
    // here `a` can not be exposed because a.y is used, which is originated
    // from `y + 1` which doesn't has a simple name. there is no way to
    // reference that column if exposed
    // (Actually we can, in some cases. In the example above, we can rewrite it
    // as 'SELECT 1 FROM b AS a WHERE a.y + 1 = 3'. Due to the complexity we
    // don't implement it)

    final SQLNode node = target.locateNodeIn(root);
    if (node == null) return false;

    final QueryScope scope = node.get(RESOLVED_QUERY_SCOPE);
    final TableSource source = node.get(RESOLVED_TABLE_SOURCE);

    final Set<ColumnRef> usedColumn = ColumnAccessAnalyzer.analyze(scope.queryNode(), source, false);
    for (ColumnRef columnRef : usedColumn) {
      assert source.equals(columnRef.source()) && columnRef.refItem() != null;
      if (exprKind(columnRef.refItem().expr()) != SQLExpr.Kind.COLUMN_REF) return false;
      assert columnRef.refItem().simpleName() != null;
    }

    return true;
  }

  @Override
  public boolean isValid(SQLNode node) {
    return relationGraph.graph().nodes().contains(target) && target.locateNodeIn(node) != null;
  }

  @Override
  public Relation target() {
    return target;
  }

  @Override
  public void modifyGraph(SQLNode root) {
    final SQLNode parent = target.node().parent();
    final var graph = relationGraph.graph();

    if (isJoined(parent)) {
      // avoid ConcurrentModificationException
      final Set<Relation> neighbours = new HashSet<>(graph.adjacentNodes(target));
      removedConds = new HashSet<>(neighbours.size());
      addedConds = new HashSet<>(neighbours.size());

      for (Relation neighbour : neighbours) {
        final JoinCondition joinCondition = graph.removeEdge(target, neighbour);
        final JoinCondition newJoinCondition = rebuildJoinCondition(root, joinCondition);
        graph.putEdgeValue(newJoinCondition.left(), newJoinCondition.right(), newJoinCondition);

        removedConds.add(joinCondition);
        addedConds.add(newJoinCondition);
      }
    }

    graph.removeNode(target);
  }

  @Override
  public void undoModifyGraph() {
    final var graph = relationGraph.graph();
    graph.addNode(target);
    if (addedConds != null)
      for (JoinCondition addedCond : addedConds)
        graph.removeEdge(addedCond.left(), addedCond.right());
    if (removedConds != null)
      for (JoinCondition removedCond : removedConds)
        graph.putEdgeValue(removedCond.left(), removedCond.right(), removedCond);
  }

  @Override
  public SQLNode modifyAST(Statement stmt, SQLNode root) {
    final SQLNode targetNode = target.locateNodeIn(root);
    final TableSource targetSource = targetNode.get(RESOLVED_TABLE_SOURCE);
    final SQLNode innerQuery = targetNode.get(DERIVED_SUBQUERY);
    final SQLNode innerQuerySpec = innerQuery.get(QUERY_BODY); // QUERY_SPEC
    final QueryScope innerScope = innerQuerySpec.get(RESOLVED_QUERY_SCOPE);
    final QueryScope outerScope = targetNode.get(RESOLVED_QUERY_SCOPE);
    final SQLNode outerQuery = outerScope.queryNode();
    final SQLNode parent = targetNode.parent();
    final SQLNode condNode = isJoined(parent) ? parent.get(JOINED_ON) : null;
    final JoinType joinType = isJoined(parent) ? parent.get(JOINED_TYPE) : null;
    final SQLNode fromNode = innerQuerySpec.get(QUERY_SPEC_FROM);
    final SQLNode whereNode = innerQuerySpec.get(QUERY_SPEC_WHERE);

    final boolean isOnlyTable = targetNode.parent().type() == Type.QUERY_SPEC;

    // e.g. select * from (select a.i as x from a where a.j = 1) b where b.x = 3

    // 1. modify column ref name
    //    => select * from (select a.i as x from a where a.j = 1) b where b.i = 3
    ExposeTableSourceName.build(targetSource).apply(outerQuery);

    // 2. assign alias to each inner table and rename its refs
    //    => select * from (select a.i as x from a AS `a_exposed_1_1` where `a_exposed_1_1`.j = 1)
    //       where `a_exposed_1_1`.i = 3
    for (TableSource source : innerScope.tableSources().values()) {
      final String newAlias = genAlias(outerScope, source);
      source.putAlias(newAlias);
      // recursive resolution is needed since afterwards
      // the inner tables will be exposed
      RenameTableSource.build(source, newAlias, true).apply(outerQuery);
    }

    // 3. remove the derived table
    //   => select * from where b.i = 3
    DropTableSource.build(targetSource).apply(outerQuery);

    // 4. add the inner tables
    //    => select * from a AS `a_exposed_0` where `a_exposed`.i = 3
    // we can just reuse the existing ON-condition node without any adjust
    // since in step 1 and 2 the table and column name has been corrected
    AppendTableSource.build(fromNode, condNode, joinType).apply(outerQuery);

    // 5. move the inner predicate to outer
    //    => select * from a AS `a_exposed_0` where `a_exposed`.i = 3 and `a_exposed_0`.j = 1
    // again, all names must be already corrected
    if (whereNode != null) AppendPredicateToClause.build(whereNode, WHERE, AND).apply(outerQuery);

    if (isOnlyTable) mergeOrderAndLimit(outerQuery, innerQuery);

    Resolve.build().apply(stmt);
    return root;
  }

  private String genAlias(QueryScope outerScope, TableSource source) {
    int suffix = 1;
    final String prefix = source.name() + "_exposed_" + (outerScope.level() + 1) + "_";

    String name = prefix + suffix;
    while (outerScope.resolveTable(name, true).left() != null) {
      ++suffix;
      name = prefix + suffix;
    }
    return name;
  }

  private JoinCondition rebuildJoinCondition(SQLNode root, JoinCondition cond) {
    final Relation thisRelation = cond.thisRelation(target);
    final Relation otherRelation = cond.thatRelation(target);
    final String thisColumn = cond.thisColumn(target);
    final String otherColumn = cond.thatColumn(target);

    final SelectItem item =
        thisRelation.locateNodeIn(root).get(RESOLVED_TABLE_SOURCE).resolveAsSelection(thisColumn);
    assert item != null;

    final ColumnRef columnRef = item.expr().get(RESOLVED_COLUMN_REF);
    final Relation newThisRelation = Relation.of(columnRef.source().node());
    final String newThisColumn = item.simpleName();

    return JoinCondition.of(
        cond.node(), newThisRelation, otherRelation, newThisColumn, otherColumn);
  }

  private void mergeOrderAndLimit(SQLNode outer, SQLNode inner) {
    final SQLNode innerLimit = inner.get(QUERY_LIMIT);
    final SQLNode outerLimit = outer.get(QUERY_LIMIT);

    final SQLNode innerOffset = inner.get(QUERY_OFFSET);
    final SQLNode outerOffset = outer.get(QUERY_OFFSET);

    final List<SQLNode> innerOrderBy = inner.get(QUERY_ORDER_BY);
    final List<SQLNode> outerOrderBy = outer.get(QUERY_ORDER_BY);

    if (innerLimit == null) return;

    outer.put(QUERY_ORDER_BY, mergeOrderBy(outerOrderBy, innerOrderBy));
    outer.put(QUERY_LIMIT, mergeLimit(outerLimit, innerLimit));
    outer.put(QUERY_OFFSET, mergeOffset(outerOffset, innerOffset));
  }

  private SQLNode mergeLimit(SQLNode outerLimit, SQLNode innerLimit) {
    if (outerLimit == null) return innerLimit;

    final Object outerValue = outerLimit.get(LITERAL_VALUE);
    final Object innerValue = innerLimit.get(LITERAL_VALUE);
    if (!(outerValue instanceof Integer)) return outerLimit;
    if (!(innerValue instanceof Integer)) return innerLimit;
    return (int) outerValue < (int) innerValue ? outerLimit : innerLimit;
  }

  private SQLNode mergeOffset(SQLNode outerOffset, SQLNode innerOffset) {
    if (outerOffset == null) return innerOffset;
    if (innerOffset == null) return outerOffset;
    final Object outerValue = outerOffset.get(LITERAL_VALUE);
    final Object innerValue = innerOffset.get(LITERAL_VALUE);
    if (!(outerValue instanceof Integer)) return outerOffset;
    if (!(innerValue instanceof Integer)) return innerOffset;
    return literal(LiteralType.INTEGER, (int) outerValue + (int) innerValue);
  }

  private List<SQLNode> mergeOrderBy(List<SQLNode> outerOrderBy, List<SQLNode> innerOrderBy) {
    if (isEmpty(outerOrderBy)) return innerOrderBy;
    if (isEmpty(innerOrderBy)) return outerOrderBy;
    final List<SQLNode> orderBy = new ArrayList<>(outerOrderBy.size() + innerOrderBy.size());
    orderBy.addAll(innerOrderBy);
    orderBy.addAll(outerOrderBy);
    return orderBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ExposeDerivedTableSource that = (ExposeDerivedTableSource) o;
    return Objects.equals(target, that.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(target);
  }
}
