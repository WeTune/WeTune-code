package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.TableAccessAnalyzer;
import sjtu.ipads.wtune.stmt.attrs.*;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.operators.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.BinaryOp.AND;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.exprKind;
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
    // from `y + 1` which doesn't has a simple name

    final SQLNode node = target.locateNodeIn(root);
    if (node == null) return false;

    final QueryScope scope = node.get(RESOLVED_QUERY_SCOPE);
    final TableSource source = node.get(RESOLVED_TABLE_SOURCE);

    final Set<ColumnRef> usedColumn = TableAccessAnalyzer.analyze(scope.queryNode(), source, false);
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
    final SQLNode innerQuery = targetNode.get(DERIVED_SUBQUERY).get(QUERY_BODY); // QUERY_SPEC
    final QueryScope innerScope = innerQuery.get(RESOLVED_QUERY_SCOPE);
    final QueryScope outerScope = targetNode.get(RESOLVED_QUERY_SCOPE);
    final SQLNode outerQuery = outerScope.queryNode();
    final SQLNode parent = targetNode.parent();
    final SQLNode condNode = isJoined(parent) ? parent.get(JOINED_ON) : null;
    final JoinType joinType = isJoined(parent) ? parent.get(JOINED_TYPE) : null;
    final SQLNode fromNode = innerQuery.get(QUERY_SPEC_FROM);
    final SQLNode whereNode = innerQuery.get(QUERY_SPEC_WHERE);

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
