package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.DependentQueryAnalyzer;
import sjtu.ipads.wtune.stmt.analyzer.NodeFinder;
import sjtu.ipads.wtune.stmt.attrs.JoinCondition;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.Relation;
import sjtu.ipads.wtune.stmt.attrs.RelationGraph;
import sjtu.ipads.wtune.stmt.resovler.IdResolver;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.operators.AddTableSource;
import sjtu.ipads.wtune.systhesis.operators.RemovePredicate;
import sjtu.ipads.wtune.systhesis.operators.Resolve;

import java.util.Objects;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.Kind.DERIVED;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class InlineSubquery implements RelationMutator {
  private final RelationGraph relationGraph;
  private final Relation target;
  private final JoinCondition cond;

  public InlineSubquery(final RelationGraph relationGraph, Relation target) {
    assert relationGraph != null && target != null && !target.isTableSource();

    final var graph = relationGraph.graph();
    // invariant 1: |independent_neighbours| <= |neighbours|
    // invariant 2: for subquery relation, its |neighbours| <= 1
    // since we have checked independent_neighbours == 1 in `canInline`
    // its |neighbours| is guaranteed to be 1
    final Set<Relation> neighbour = graph.adjacentNodes(target);
    assert graph.nodes().contains(target) && neighbour.size() == 1;

    this.relationGraph = relationGraph;
    this.target = target;
    this.cond = graph.edgeValue(target, neighbour.iterator().next()).orElse(null);

    assert cond != null;
  }

  public static boolean canInline(SQLNode root, RelationGraph graph, Relation target) {
    if (target.isTableSource()) return false;
    final SQLNode subquery = target.locateNodeIn(root);
    if (subquery == null || DependentQueryAnalyzer.isDependent(subquery)) return false;
    return graph.independentNeighbours(root, target).size() == 1;
  }

  private String genAlias(QueryScope outerScope) {
    int suffix = 1;
    final String prefix = "_inlined_" + (outerScope.level() + 1) + "_";
    String name = prefix + suffix;
    while (outerScope.resolveTable(name, true).left() != null) {
      ++suffix;
      name = prefix + suffix;
    }
    return name;
  }

  @Override
  public boolean isValid(SQLNode root) {
    return target.locateNodeIn(root) != null;
  }

  @Override
  public Relation target() {
    return target;
  }

  @Override
  public void modifyGraph(SQLNode root) {
    target.setGeneratedNode(new SQLNode(SQLNode.Type.INVALID)); // just a placeholder
  }

  @Override
  public void undoModifyGraph() {
    target.setGeneratedNode(null);
  }

  @Override
  public SQLNode modifyAST(Statement stmt, SQLNode root) {
    final SQLNode outerQuery =
        NodeFinder.find(root, target.originalNode()).get(RESOLVED_QUERY_SCOPE).parent().queryNode();
    assert outerQuery != null;

    final SQLNode newTableSource = genTableSource(outerQuery);
    final SQLNode joinCond = genJoinCondition(outerQuery, newTableSource);

    RemovePredicate.build(target.originalNode().parent()).apply(outerQuery);
    // Note: impl of AddTableSource modifies the newTableSource object in-place
    //       to connect a JOIN. we need to retrieve the reference to the new node
    final AddTableSource addTableOp =
        AddTableSource.build(newTableSource, joinCond, JoinType.INNER_JOIN);
    addTableOp.apply(outerQuery);
    Resolve.build().apply(stmt);

    target.setGeneratedNode(addTableOp.pointer());
    return root;
  }

  private SQLNode genTableSource(SQLNode root) {
    final SQLNode tableSource = newTableSource(DERIVED);
    final SQLNode originalNode = NodeFinder.find(root, target.originalNode());
    final String alias = genAlias(root.get(RESOLVED_QUERY_SCOPE));

    tableSource.put(DERIVED_SUBQUERY, originalNode.copy());
    tableSource.put(DERIVED_ALIAS, alias);
    IdResolver.resolve(tableSource);

    return tableSource;
  }

  private SQLNode genJoinCondition(SQLNode root, SQLNode generatedTableSource) {
    final SQLNode inSubExpr = NodeFinder.find(root, target.originalNode()).parent();
    assert inSubExpr.get(BINARY_OP) == BinaryOp.IN_SUBQUERY;

    // use left node found in `root` since its name may have changed
    final SQLNode leftRef = inSubExpr.get(BINARY_LEFT);

    // right column name must keep the same, no need to resolve in `root`
    return binary(
        leftRef,
        columnRef(tableSourceName(generatedTableSource), cond.rightColumn()),
        SQLExpr.BinaryOp.EQUAL);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InlineSubquery that = (InlineSubquery) o;
    return Objects.equals(target, that.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(target);
  }
}
