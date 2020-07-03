package sjtu.ipads.wtune.stmt.attrs;

import com.google.common.graph.MutableValueGraph;

public class RelationGraph {
  private final QueryScope rootScope;
  private final MutableValueGraph<Relation, JoinCondition> graph;

  private RelationGraph(QueryScope rootScope, MutableValueGraph<Relation, JoinCondition> graph) {
    this.rootScope = rootScope;
    this.graph = graph;
  }

  public static RelationGraph build(
      QueryScope rootScope, MutableValueGraph<Relation, JoinCondition> graph) {
    return new RelationGraph(rootScope, graph);
  }

  public MutableValueGraph<Relation, JoinCondition> graph() {
    return graph;
  }

  @Override
  public String toString() {
    return graph.toString();
  }
}
