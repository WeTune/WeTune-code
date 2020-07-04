package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
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
    final Set<Relation> neighbour = graph.adjacentNodes(target);
    assert graph.nodes().contains(target) && neighbour.size() == 1;

    this.relationGraph = relationGraph;
    this.target = target;
    this.cond = graph.edgeValue(target, neighbour.iterator().next()).orElse(null);

    assert cond != null;
  }

  public static boolean canInline(SQLNode root, RelationGraph graph, Relation target) {
    return !target.isTableSource()
        && graph.graph().adjacentNodes(target).size() == 1
        && NodeFinder.find(root, target.node()) != null;
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
  public boolean isValid(SQLNode node) {
    return NodeFinder.find(node, target.node()) != null;
  }

  @Override
  public Relation target() {
    return target;
  }

  @Override
  public void modifyGraph() {
    target.setGeneratedNode(new SQLNode(SQLNode.Type.INVALID)); // just a placeholder
  }

  @Override
  public void undoModifyGraph() {
    target.setGeneratedNode(null);
  }

  @Override
  public SQLNode modifyAST(Statement stmt, SQLNode node) {
    final SQLNode queryNode =
        NodeFinder.find(node, target.originalNode()).get(RESOLVED_QUERY_SCOPE).parent().queryNode();

    final SQLNode tableSource = buildTableSource(queryNode);
    target.setGeneratedNode(tableSource);

    final SQLNode joinCond = buildJoinCondition(queryNode);

    RemovePredicate.build(target.originalNode().parent()).apply(queryNode);
    final AddTableSource addTableOp =
        (AddTableSource) AddTableSource.build(tableSource, joinCond, JoinType.INNER_JOIN);
    addTableOp.apply(queryNode);
    Resolve.build().apply(stmt);

    target.setGeneratedNode(addTableOp.pointer());
    return node;
  }

  private SQLNode buildTableSource(SQLNode root) {
    final SQLNode tableSource = newTableSource(DERIVED);
    final SQLNode originalNode = NodeFinder.find(root, target.originalNode());
    final String alias = genAlias(root.get(RESOLVED_QUERY_SCOPE));

    tableSource.put(DERIVED_SUBQUERY, originalNode.copy());
    tableSource.put(DERIVED_ALIAS, alias);
    IdResolver.resolve(tableSource);

    return tableSource;
  }

  private SQLNode buildJoinCondition(SQLNode root) {
    final SQLNode parent = NodeFinder.find(root, target.originalNode()).parent();
    assert exprKind(parent) == SQLExpr.Kind.BINARY && parent.get(BINARY_OP) == BinaryOp.IN_SUBQUERY;

    final SQLNode leftRef = parent.get(BINARY_LEFT);
    final SQLNode rightNode = cond.right().node();

    return binary(
        leftRef, columnRef(tableSourceName(rightNode), cond.rightColumn()), SQLExpr.BinaryOp.EQUAL);
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
