package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLTableSource;
import sjtu.ipads.wtune.stmt.analyzer.NodeFinder;
import sjtu.ipads.wtune.stmt.analyzer.ColumnAccessAnalyzer;
import sjtu.ipads.wtune.stmt.attrs.*;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.operators.DropTableSource;
import sjtu.ipads.wtune.systhesis.operators.ReplaceColumnRef;
import sjtu.ipads.wtune.systhesis.operators.Resolve;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_TABLE_SOURCE;

public class ReduceTableSource implements RelationMutator {
  private final RelationGraph relationGraph;
  private final Relation target;
  //  private JoinCondition removedEdge;

  private Set<JoinCondition> removedEdges;
  private Set<JoinCondition> addedEdges;
  private JoinCondition pivotEdge;

  public ReduceTableSource(RelationGraph relationGraph, Relation target) {
    this.relationGraph = relationGraph;
    this.target = target;
  }

  public static boolean canReduce(SQLNode root, RelationGraph graph, Relation target) {
    // Condition 1: must be a table source
    // i.e. must reside in a FROM clause
    if (!target.isTableSource()) return false;

    final SQLNode targetNode = target.locateNodeIn(root);
    if (targetNode == null) return false;

    if (isDerived(targetNode)
        && isJoined(targetNode.parent())
        && targetNode.parent().get(JOINED_TYPE).isInner()
        && targetNode.get(DERIVED_SUBQUERY).get(QUERY_BODY).get(QUERY_SPEC_WHERE) != null)
      return false;
    // Condition 2: must be not isolated
    // Because an isolated should be the only table in a FROM clause.
    //
    // MEMO
    // Only the relations that resides in the same FROM clause (as the target) is counted as
    // neighbours.
    // e.g. FROM a WHERE a.i IN (SELECT b.x FROM b) AND EXISTS (SELECT 1 FROM c WHERE c.u = a.j)
    // Apparently `a` should not be reduced in this statement.
    // Thus, although `a` is connected with two subqueries in graph, they are not counted as
    // neighbours. Then `a` is considered as isolated so won't be reduced.
    long neighboursCount =
        graph.independentNeighbours(root, target).stream().filter(Relation::isTableSource).count();
    if (neighboursCount == 0) return false;
    //    if (neighboursCount != 1) return false;

    final QueryScope scope = targetNode.get(RESOLVED_QUERY_SCOPE);
    final TableSource source = targetNode.get(RESOLVED_TABLE_SOURCE);
    // recursive table source resolution is unnecessary
    final Set<ColumnRef> columnRef = ColumnAccessAnalyzer.analyze(scope.queryNode(), source, false);
    final long usedColumnCount = columnRef.size();
    // columnRef.stream().map(ColumnRef::identity).distinct().count();

    // Condition 3: the number of used columns is 1
    // i.e. the only used column is the one in join condition
    // Since that we can reduce the table and replace this column
    // by the other side of join condition.
    return usedColumnCount <= 1;

    // MEMO
    // If a reducible relation has more than one neighbour, then all the join conditions
    // is joined on the same column of the relation.
  }

  @Override
  public boolean isValid(SQLNode root) {
    final var graph = relationGraph.graph();
    if (!graph.nodes().contains(target) || target.locateNodeIn(root) == null) return false;
    // need to re-check because it may become isolated now
    long neighboursCount =
        relationGraph.independentNeighbours(root, target).stream()
            .filter(Relation::isTableSource)
            .count();
    //    return neighboursCount == 1;
    return neighboursCount != 0;
  }

  @Override
  public Relation target() {
    return target;
  }

  @Override
  public void modifyGraph(SQLNode root) {
    // 1. find a replacement of target
    //// 1.1 find the ON expr of the JOIN.
    ////     find a join condition in the ON expr
    ////     use the another relation in the condition as replacement
    //// 1.2 otherwise, pick a random neighbour as replacement
    ////
    //// MEMO
    //// assume we want to reduce relation `b`
    //// Example 1: "FROM a JOIN b ON a.i = b.x JOIN c ON c.u = b.x"
    ////   here we must pick `a` as the replacement instead of `c`
    ////   b/c if `c` is picked, the statement will be "FROM a JOIN c ON c.u = c.u",
    ////   obviously invalid.
    //// Example 2: "FROM a, b, c WHERE a.i = b.x AND c.u = b.x"
    ////   both `a` and `c` can be the replacement.
    ////   pick `a`: "FROM a, c WHERE a.i = a.i AND c.u = a.i"
    ////   pick `c`: "FROM a, c WHERE a.i = c.u AND c.u = c.u"
    ////   Both is okay.
    final SQLNode targetNode = NodeFinder.find(root, target.node());
    final SQLNode joinNode = targetNode.parent();
    assert isJoined(joinNode);
    final SQLNode onExpr = joinNode.get(JOINED_ON);
    final var graph = relationGraph.graph();
    final Set<Relation> neighbours = new HashSet<>(graph.adjacentNodes(target));
    final Set<Relation> independentNeighbours =
        relationGraph.independentNeighbours(root, target).stream()
            .filter(Relation::isTableSource)
            .collect(Collectors.toSet());

    pivotEdge = null;
    for (Relation neighbour : neighbours) {
      if (!neighbour.isTableSource()) continue;
      final JoinCondition condition = graph.edgeValue(target, neighbour).orElseThrow();
      if (NodeFinder.find(onExpr, condition.node()) != null) {
        pivotEdge = condition;
        break;
      }
    }

    if (pivotEdge == null)
      pivotEdge = graph.edgeValue(target, independentNeighbours.iterator().next()).orElseThrow();

    assert pivotEdge != null;

    final Relation repRelation = pivotEdge.thatRelation(target);
    final String repColumn = pivotEdge.thatColumn(target);

    // 2. remove all edges between target and its neighbours
    // 3. add edges between the replacement and the original neighbours
    removedEdges = new HashSet<>(neighbours.size());
    addedEdges = new HashSet<>(neighbours.size());
    for (Relation neighbour : neighbours) {
      final JoinCondition removedEdge = graph.removeEdge(target, neighbour);
      removedEdges.add(removedEdge);

      if (neighbour != repRelation) {
        final JoinCondition addedEdge =
            JoinCondition.of(
                removedEdge.node(),
                repRelation,
                removedEdge.thatRelation(target),
                repColumn,
                removedEdge.thatColumn(target));
        graph.putEdgeValue(repRelation, neighbour, addedEdge);

        addedEdges.add(addedEdge);
      }
    }

    // 4. remove target
    graph.removeNode(target);
  }

  @Override
  public void undoModifyGraph() {
    final var graph = relationGraph.graph();
    graph.addNode(target);
    for (JoinCondition addedEdge : addedEdges)
      graph.removeEdge(addedEdge.left(), addedEdge.right());
    for (JoinCondition removedEdge : removedEdges)
      graph.putEdgeValue(removedEdge.left(), removedEdge.right(), removedEdge);
    addedEdges = null;
    removedEdges = null;
  }

  @Override
  public SQLNode modifyAST(Statement stmt, SQLNode root) {
    //    assert removedEdge != null;
    assert pivotEdge != null && removedEdges != null && addedEdges != null;
    final String otherRelName = tableSourceName(pivotEdge.thatRelation(target).locateNodeIn(root));
    final String otherColumn = pivotEdge.thatColumn(target);

    final SQLNode targetNode = target.locateNodeIn(root);
    final QueryScope scope = targetNode.get(RESOLVED_QUERY_SCOPE);
    final SQLNode queryNode = scope.queryNode();
    final TableSource source = targetNode.get(RESOLVED_TABLE_SOURCE);
    final Set<ColumnRef> columnRefs = ColumnAccessAnalyzer.analyze(queryNode, source, false);
    // assert 1 == columnRef.stream().map(ColumnRef::identity).distinct().count();
    final ColumnRef columnRef = columnRefs.iterator().next();

    ReplaceColumnRef.build(columnRef, otherRelName, otherColumn).apply(queryNode);
    DropTableSource.build(source).apply(queryNode);
    Resolve.build().apply(stmt);

    return root;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReduceTableSource that = (ReduceTableSource) o;
    return Objects.equals(target, that.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(target);
  }
}
