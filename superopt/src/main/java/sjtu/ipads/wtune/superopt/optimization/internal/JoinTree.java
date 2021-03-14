package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.Commons.tail;

public class JoinTree {
  private List<JoinNode> nodes;
  private final PlanNode originalRoot;
  private final PlanNode predecessor;

  public JoinTree(List<JoinNode> nodes, PlanNode predecessor) {
    this.nodes = nodes;
    this.originalRoot = tail(nodes);
    this.predecessor = predecessor;
  }

  public static JoinTree make(JoinNode root) {
    final List<JoinNode> nodes = new ArrayList<>();

    PlanNode predecessor = root;
    while (predecessor.type().isJoin()) {
      nodes.add((JoinNode) predecessor);
      predecessor = predecessor.predecessors()[0];
    }

    Collections.reverse(nodes);
    return new JoinTree(nodes, predecessor);
  }

  public PlanNode originalRoot() {
    return originalRoot;
  }

  public JoinNode rebuild() {
    nodes.get(0).setPredecessor(0, predecessor);
    for (int i = 0, bound = nodes.size() - 1; i < bound; i++)
      nodes.get(i + 1).setPredecessor(0, nodes.get(i));

    return tail(nodes);
  }

  public JoinTree sort() {
    nodes.sort(Comparator.comparing(PlanNode::toString));
    final List<JoinNode> sorted = new ArrayList<>();
    bubble(nodes, -1, sorted);
    nodes = sorted;
    return this;
  }

  private void bubble(List<JoinNode> nodes, int i, List<JoinNode> sorted) {
    if (nodes.isEmpty()) return;
    if (i >= sorted.size()) {
      for (JoinNode node : nodes) if (node != null) sorted.add(node);
      return;
    }

    final PlanNode pivot = i == -1 ? predecessor : sorted.get(i).predecessors()[1];
    // we assume no such wired join: a JOIN b ON .. JOIN c ON a.x = c.z AND b.y = c.w
    final ListIterator<JoinNode> iter = nodes.listIterator();
    while (iter.hasNext()) {
      final JoinNode join = iter.next();
      if (join != null && pivot.definedAttributes().containsAll(join.leftAttributes())) {
        iter.set(null);
        sorted.add(join);
      }
    }

    bubble(nodes, i + 1, sorted);
  }
}
