package sjtu.ipads.wtune.superopt.optimizer.join;

import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.copyOnTree;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.optimizer.OptimizerException;

public class JoinTreeImpl extends AbstractList<JoinNode> implements JoinTree {
  // Assumption: no such wired join: a JOIN b ON .. JOIN c ON a.x = c.z AND b.y = c.w

  private List<JoinNode> joins;
  private final PlanNode originalRoot;
  private final PlanNode predecessor;
  private final boolean swapLeaf;

  private int[] dependency;

  private JoinTreeImpl(List<JoinNode> nodes, PlanNode predecessor, boolean swapLeaf) {
    this.joins = nodes;
    this.originalRoot = tail(nodes);
    this.predecessor = predecessor;
    this.swapLeaf = swapLeaf;
  }

  public static JoinTreeImpl make(JoinNode root) {
    final List<JoinNode> nodes = new ArrayList<>();

    PlanNode predecessor = root;
    while (predecessor.kind().isJoin()) {
      nodes.add((JoinNode) predecessor);
      predecessor = predecessor.predecessors()[0];
    }

    Collections.reverse(nodes);
    return new JoinTreeImpl(nodes, predecessor, false);
  }

  @Override
  public PlanNode originalRoot() {
    return originalRoot;
  }

  @Override
  public JoinNode rebuild() {
    joins.get(0).setPredecessor(0, predecessor);
    for (int i = 0, bound = joins.size() - 1; i < bound; i++)
      joins.get(i + 1).setPredecessor(0, joins.get(i));

    final JoinNode newRoot = tail(joins);
    if (swapLeaf) swapLeaf(newRoot);
    return newRoot;
  }

  @Override
  public JoinNode copyAndRebuild() {
    PlanNode root = joins.get(0).copy();
    root.setPredecessor(0, copyOnTree(predecessor));
    root.setPredecessor(1, copyOnTree(root.predecessors()[1]));

    for (int i = 0, bound = joins.size() - 1; i < bound; i++) {
      final PlanNode join = joins.get(i + 1).copy();
      join.setPredecessor(0, root);
      join.setPredecessor(1, copyOnTree(join.predecessors()[1]));
      root = join;
    }

    if (swapLeaf) swapLeaf((JoinNode) root);
    //    resolveUsedOnTree(root); // necessary?
    return (JoinNode) root;
  }

  @Override
  public JoinTree withRoot(int rootIdx) {
    if (rootIdx == -1 && swapLeaf) throw new IllegalStateException();

    final boolean swapLeaf = rootIdx < 0;
    rootIdx = Math.max(rootIdx, 0);

    final List<JoinNode> newJoins = new ArrayList<>(joins.size());
    for (int i = 0; i < joins.size(); i++) if (i != rootIdx) newJoins.add(joins.get(i));
    newJoins.add(joins.get(rootIdx));
    return new JoinTreeImpl(newJoins, predecessor, swapLeaf);
  }

  @Override
  public JoinTree sorted() {
    if (swapLeaf) throw new IllegalStateException();

    joins.sort(Comparator.comparing(PlanNode::toString));
    final List<JoinNode> sorted = new ArrayList<>();
    bubbleSort(joins, -1, sorted);
    joins = sorted;
    return this;
  }

  @Override
  public boolean isValidRoot(int rootIdx) {
    final int[] dependency = dependency();
    for (int i = Math.max(rootIdx, 0) + 1, bound = dependency.length; i < bound; ++i)
      if (dependency[i] == rootIdx) return false;
    return true;
  }

  private int[] dependency() {
    if (swapLeaf) throw new IllegalStateException();
    if (dependency != null) return dependency;

    final int[] dependency = new int[joins.size()];
    Arrays.fill(dependency, -2);

    for (int i = -1, bound = joins.size() - 1; i < bound; i++) {
      final PlanNode source = i == -1 ? predecessor : joins.get(i).predecessors()[1];

      outer:
      for (int j = i + 1; j <= bound; j++) {
        final List<AttributeDef> usedAttrs = joins.get(j).leftAttributes();
        if (usedAttrs.isEmpty()) throw new OptimizerException();

        for (AttributeDef usedAttr : usedAttrs)
          if (source.definedAttributes().stream().noneMatch(it -> it == usedAttr)) continue outer;

        assert dependency[j] == -2;
        dependency[j] = i;
      }
    }

    if (!checkDependency(dependency)) throw new OptimizerException();

    return this.dependency = dependency;
  }

  private void bubbleSort(List<JoinNode> nodes, int i, List<JoinNode> sorted) {
    if (nodes.isEmpty()) return;
    if (i >= sorted.size()) {
      for (JoinNode node : nodes) if (node != null) sorted.add(node);
      return;
    }

    final PlanNode pivot = i == -1 ? predecessor : sorted.get(i).predecessors()[1];
    final ListIterator<JoinNode> iter = nodes.listIterator();
    while (iter.hasNext()) {
      final JoinNode join = iter.next();
      if (join != null && pivot.definedAttributes().containsAll(join.leftAttributes())) {
        iter.set(null);
        sorted.add(join);
      }
    }

    bubbleSort(nodes, i + 1, sorted);
  }

  @Override
  public JoinNode get(int index) {
    return joins.get(index);
  }

  @Override
  public int size() {
    return joins.size();
  }

  private static void swapLeaf(JoinNode root) {
    final PlanNode leftMostLeaf = JoinTree.predecessorOfTree(root);
    final PlanNode rightMostLeaf = root.predecessors()[1];
    final PlanNode tail = leftMostLeaf.successor();
    tail.setPredecessor(0, rightMostLeaf);
    root.setPredecessor(1, leftMostLeaf);
    root.resolveUsed(); // the two-side attributes are swapped, re-resolve
  }

  private static boolean checkDependency(int[] dependency) {
    for (int i : dependency) if (i == -2) return false;
    return true;
  }
}
