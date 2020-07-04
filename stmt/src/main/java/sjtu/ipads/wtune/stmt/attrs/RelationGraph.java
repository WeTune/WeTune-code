package sjtu.ipads.wtune.stmt.attrs;

import com.google.common.graph.MutableValueGraph;

public class RelationGraph {
  private final MutableValueGraph<Relation, JoinCondition> graph;

  private RelationGraph(MutableValueGraph<Relation, JoinCondition> graph) {
    this.graph = graph;
  }

  public static RelationGraph build(MutableValueGraph<Relation, JoinCondition> graph) {
    return new RelationGraph(graph);
  }

  public MutableValueGraph<Relation, JoinCondition> graph() {
    return graph;
  }

  @Override
  public String toString() {
    return graph.toString();
  }
}
