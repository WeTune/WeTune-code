package sjtu.ipads.wtune.stmt.attrs;

import com.google.common.graph.MutableValueGraph;
import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

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

  public Set<Relation> independentNeighbours(SQLNode root, Relation relation) {
    final Set<Relation> neighbours = graph.adjacentNodes(relation);
    final SQLNode thisNode = relation.locateNodeIn(root);
    final Set<Relation> ret = new HashSet<>(neighbours.size());
    for (Relation neighbour : neighbours)
      if (scopeOf(thisNode) == scopeOf(neighbour.locateNodeIn(root))) ret.add(neighbour);
    return ret;
  }

  private static QueryScope scopeOf(SQLNode node) {
    final QueryScope scope = node.get(RESOLVED_QUERY_SCOPE);
    return node.type() == SQLNode.Type.QUERY ? scope.parent() : scope;
  }

  @Override
  public String toString() {
    return graph.toString();
  }
}
