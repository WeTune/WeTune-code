package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.NodeFinder;
import sjtu.ipads.wtune.stmt.analyzer.TableAccessAnalyzer;
import sjtu.ipads.wtune.stmt.attrs.*;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.operators.*;

import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.BinaryOp.AND;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.exprKind;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.Kind.SIMPLE;
import static sjtu.ipads.wtune.stmt.attrs.QueryScope.Clause.WHERE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;

public class ExposeDerivedTableSource {
  private final RelationGraph relationGraph;
  private final Relation target;
  private Set<JoinCondition> removedConds;
  private Set<JoinCondition> addedConds;

  public ExposeDerivedTableSource(final RelationGraph relationGraph, Relation target) {
    this.relationGraph = relationGraph;
    this.target = target;
  }

  public static boolean canExpose(Relation target) {
    // exclude non table source
    if (!target.isTableSource()) return false;
    // exclude simple source
    if (target.node().get(TABLE_SOURCE_KIND) == SIMPLE) return false;
    // exclude UNION
    if (target.node().get(DERIVED_SUBQUERY).get(QUERY_BODY).type() != Type.QUERY_SPEC) return false;

    // condition:
    // the used column of this derived table source must be originated from a column ref expr.
    // e.g. SELECT 1 FROM (SELECT x AS x, y + 1 AS y FROM b) AS a WHERE a.y = 3
    // here `a` can not be exposed because a.y is used, which is originated
    // from `y + 1`, not column ref expr
    final SQLNode node = target.node();
    final QueryScope scope = node.get(RESOLVED_QUERY_SCOPE);
    final TableSource source = node.get(RESOLVED_TABLE_SOURCE);

    final Set<ColumnRef> usedColumn = TableAccessAnalyzer.analyze(scope.queryNode(), source, false);
    for (ColumnRef columnRef : usedColumn) {
      assert source.equals(columnRef.source()) && columnRef.refItem() != null;
      if (exprKind(columnRef.refItem().expr()) != SQLExpr.Kind.COLUMN_REF) return false;
    }

    return true;
  }

  private String genAlias(QueryScope scope, TableSource source) {
    int suffix = 0;
    final String prefix = source.name() + "_exposed_";
    String name = prefix + suffix;
    while (scope.resolveTable(name, true).left() != null) {
      ++suffix;
      name = prefix + suffix;
    }
    return name;
  }

  void modifyGraph() {
    final SQLNode parent = target.node().parent();
    final var graph = relationGraph.graph();

    if (isJoined(parent)) {
      final Set<Relation> neighbours = graph.adjacentNodes(target);
      removedConds = new HashSet<>(neighbours.size());
      addedConds = new HashSet<>(neighbours.size());

      for (Relation neighbour : neighbours) {
        final JoinCondition joinCondition = graph.removeEdge(target, neighbour);
        final JoinCondition newJoinCondition = resolveJoinCondition(joinCondition);
        graph.putEdgeValue(newJoinCondition.left(), newJoinCondition.right(), newJoinCondition);

        removedConds.add(joinCondition);
        addedConds.add(newJoinCondition);
      }
    }

    graph.removeNode(target);
  }

  void undoModifyGraph() {
    final var graph = relationGraph.graph();
    graph.addNode(target);
    for (JoinCondition addedCond : addedConds)
      graph.removeEdge(addedCond.left(), addedCond.right());
    for (JoinCondition removedCond : removedConds)
      graph.putEdgeValue(removedCond.left(), removedCond.right(), removedCond);
  }

  private JoinCondition resolveJoinCondition(JoinCondition cond) {
    final Relation thisRelation = cond.thisRelation(target);
    final Relation otherRelation = cond.thatRelation(target);
    final String thisColumn = cond.thisColumn(target);
    final String otherColumn = cond.thatColumn(target);

    final SelectItem item =
        thisRelation.node().get(RESOLVED_TABLE_SOURCE).resolveAsSelection(thisColumn);
    assert item != null;

    final ColumnRef columnRef = item.expr().get(RESOLVED_COLUMN_REF);
    final Relation newThisRelation = Relation.of(columnRef.source().node());
    final String newThisColumn = item.simpleName();

    return JoinCondition.of(newThisRelation, otherRelation, newThisColumn, otherColumn);
  }

  SQLNode modifyAST(Statement stmt, SQLNode root) {
    final SQLNode targetNode = NodeFinder.find(root, target.node());
    final SQLNode subquery = targetNode.get(DERIVED_SUBQUERY).get(QUERY_BODY);
    final TableSource targetSource = targetNode.get(RESOLVED_TABLE_SOURCE);
    final QueryScope outerScope = targetNode.get(RESOLVED_QUERY_SCOPE);
    final QueryScope innerScope = subquery.get(RESOLVED_QUERY_SCOPE);
    final SQLNode outerQuery = outerScope.queryNode();
    final SQLNode parent = targetNode.parent();
    final SQLNode condNode = isJoined(parent) ? parent.get(JOINED_ON) : null;
    final JoinType joinType = isJoined(parent) ? parent.get(JOINED_TYPE) : null;
    final SQLNode fromNode = subquery.get(QUERY_SPEC_FROM);
    final SQLNode whereNode = subquery.get(QUERY_SPEC_WHERE);

    // e.g. select * from (select a.i as x from a where a.j = 1) b where b.x = 3

    // 1. modify column ref name
    //    => select * from (select a.i as x from a where a.j = 1) b where b.i = 3
    InlineRefName.build(targetSource).apply(outerQuery);

    // 2. assign alias to each inner table and rename its refs
    //    => select * from (select a.i as x from a AS `a_exposed_0` where `a_exposed_0`.j = 1)
    //       where `a_exposed`.i = 3
    for (TableSource source : innerScope.tableSources().values()) {
      final String newAlias = genAlias(outerScope, source);
      source.putAlias(newAlias);
      RenameTableSource.build(source, newAlias, true).apply(outerQuery);
    }

    // 3. remove the derived table
    //   => select * from where b.i = 3
    RemoveTableSource.build(targetSource).apply(outerQuery);

    // 4. add the inner tables
    //    => select * from a AS `a_exposed_0` where `a_exposed`.i = 3
    AddTableSource.build(fromNode, condNode, joinType).apply(outerQuery);

    // 5. add the inner predicate to outer
    //    => select * from a AS `a_exposed_0` where `a_exposed`.i = 3 and `a_exposed_0`.j = 1
    if (whereNode != null) AddPredicateToClause.build(whereNode, WHERE, AND).apply(outerQuery);

    Resolve.build().apply(stmt);
    return root;
  }
}
