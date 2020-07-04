package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.JoinCondition;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.Relation;
import sjtu.ipads.wtune.stmt.attrs.RelationGraph;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.operators.AddTableSource;
import sjtu.ipads.wtune.systhesis.operators.RemovePredicate;
import sjtu.ipads.wtune.systhesis.operators.Resolve;

import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.Kind.DERIVED;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class InlineSubquery {
  private final RelationGraph relationGraph;
  private final Relation target;
  private final JoinCondition cond;

  public InlineSubquery(final RelationGraph relationGraph, Relation target) {
    assert !target.isTableSource();

    final var graph = relationGraph.graph();
    final Set<Relation> neighbour = graph.adjacentNodes(target);
    assert graph.nodes().contains(target) && neighbour.size() == 1;

    this.relationGraph = relationGraph;
    this.target = target;
    this.cond = graph.edgeValue(target, neighbour.iterator().next()).orElse(null);

    assert cond != null;
  }

  private String genAlias(QueryScope scope) {
    int suffix = scope.tableSources().size();
    final String prefix = "_inlined_" + scope.level() + "_";
    String name = prefix + suffix;
    while (scope.resolveTable(name, true).left() != null) {
      ++suffix;
      name = prefix + suffix;
    }
    return name;
  }

  void modifyGraph() {
    final SQLNode tableSource = newTableSource(DERIVED);
    final SQLNode originalNode = target.originalNode();

    final String alias = genAlias(originalNode.get(RESOLVED_QUERY_SCOPE));
    tableSource.put(DERIVED_SUBQUERY, originalNode.copy());
    tableSource.put(DERIVED_ALIAS, alias);

    target.setGeneratedNode(tableSource);
  }

  void undoModifyGraph() {
    target.setGeneratedNode(null);
  }

  SQLNode modifyAST(Statement stmt, SQLNode node) {
    RemovePredicate.build(target.originalNode().parent()).apply(node);
    AddTableSource.build(target.generatedNode(), cond.toBinary(), JoinType.INNER_JOIN).apply(node);
    Resolve.build().apply(stmt);
    return node;
  }
}
