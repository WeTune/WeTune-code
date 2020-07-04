package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.NodeFinder;
import sjtu.ipads.wtune.stmt.analyzer.TableAccessAnalyzer;
import sjtu.ipads.wtune.stmt.attrs.*;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.operators.RemoveTableSource;
import sjtu.ipads.wtune.systhesis.operators.ReplaceColumnRef;
import sjtu.ipads.wtune.systhesis.operators.Resolve;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.sqlparser.SQLTableSource.tableSourceName;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_TABLE_SOURCE;

public class ReduceTableSource implements RelationMutator {
  private final RelationGraph relationGraph;
  private final Relation target;
  private JoinCondition removedEdge;

  public ReduceTableSource(RelationGraph relationGraph, Relation target) {
    this.relationGraph = relationGraph;
    this.target = target;
  }

  public static boolean canReduce(SQLNode root, RelationGraph graph, Relation target) {
    // exclude non table source
    if (!target.isTableSource()) return false;

    // only the "leaf" node can be reduced
    final var g = graph.graph();
    long neighboursCount = g.adjacentNodes(target).stream().filter(Relation::isTableSource).count();
    if (neighboursCount != 1) return false;

    final SQLNode targetNode = NodeFinder.find(root, target.node());
    if (targetNode == null) return false;

    final QueryScope scope = targetNode.get(RESOLVED_QUERY_SCOPE);
    final TableSource source = targetNode.get(RESOLVED_TABLE_SOURCE);
    final Set<ColumnRef> columnRef = TableAccessAnalyzer.analyze(scope.queryNode(), source, false);
    final long usedColumnCount = columnRef.stream().map(ColumnRef::identity).distinct().count();

    // just need to check the count of used column
    // since the join condition itself counts 1
    return usedColumnCount <= 1;
  }

  @Override
  public boolean isValid(SQLNode node) {
    final var graph = relationGraph.graph();
    if (!graph.nodes().contains(target) || NodeFinder.find(node, target.node()) == null)
      return false;
    long neighboursCount =
        graph.adjacentNodes(target).stream().filter(Relation::isTableSource).count();
    return neighboursCount == 1;
  }

  @Override
  public Relation target() {
    return target;
  }

  @Override
  public void modifyGraph() {
    final var graph = relationGraph.graph();
    final Set<Relation> neighbours =
        graph.adjacentNodes(target).stream()
            .filter(Relation::isTableSource)
            .collect(Collectors.toSet());
    assert neighbours.size() == 1;
    final Relation neighbour = neighbours.iterator().next();
    removedEdge = graph.removeEdge(target, neighbour);
    graph.removeNode(target);
  }

  @Override
  public void undoModifyGraph() {
    relationGraph.graph().addNode(target);
    relationGraph.graph().putEdgeValue(removedEdge.left(), removedEdge.right(), removedEdge);
  }

  @Override
  public SQLNode modifyAST(Statement stmt, SQLNode root) {
    assert removedEdge != null;
    final Relation otherRelation = removedEdge.thatRelation(target);
    final String otherColumn = removedEdge.thatColumn(target);
    final String otherRelName = tableSourceName(NodeFinder.find(root, otherRelation.node()));

    final SQLNode targetNode = NodeFinder.find(root, target.node());
    final QueryScope scope = targetNode.get(RESOLVED_QUERY_SCOPE);
    final SQLNode queryNode = scope.queryNode();
    final TableSource source = targetNode.get(RESOLVED_TABLE_SOURCE);
    final Set<ColumnRef> columnRefs = TableAccessAnalyzer.analyze(queryNode, source, false);
    // assert 1 == columnRef.stream().map(ColumnRef::identity).distinct().count();

    final ColumnRef columnRef = columnRefs.iterator().next();

    ReplaceColumnRef.build(columnRef, otherRelName, otherColumn).apply(queryNode);
    RemoveTableSource.build(source).apply(queryNode);
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
