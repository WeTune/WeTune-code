package sjtu.ipads.wtune.superopt.optimizer2;

import sjtu.ipads.wtune.common.utils.ArraySupport;
import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.sqlparser.plan1.*;

import java.util.List;

import static java.lang.Integer.max;
import static sjtu.ipads.wtune.common.tree.TreeSupport.indexOfChild;
import static sjtu.ipads.wtune.common.utils.IterableSupport.any;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind.LEFT_JOIN;

/**
 * A data structure representing a left-deep join tree.
 *
 * <pre>
 * Join{A.z=C.w}(Join{A.x=B.y}(Input{A}, Input{B}), Input{C})
 * joiners[0]: Join{A.x=B.y}, [1]: Join{A.z=C.w}
 * joinees[-1]: Input{A}, [0]: Input{B}, [1]: Input{C}
 * </pre>
 *
 * Invariant: i-th joinee is the RHS of i-th joiner.
 *
 * <p>invariant: joinees.length == joiners.length + 1
 */
final class LinearJoinTree {
  private final PlanContext plan;
  private final int[] joiners; // JoinNodes.
  private final int[] joinees; // joined PlanNodes.
  // dependency[i] == j: i-th joinee's LHS join keys come from j-th joinee.
  // e.g., A Join B On p(A,B) Join C On p(B,C), dependencies=[-1,0,1]
  private final Lazy<int[]> dependencies;

  private LinearJoinTree(PlanContext plan, int[] joiners, int[] joinees) {
    this.plan = plan;
    this.joiners = joiners;
    this.joinees = joinees;
    this.dependencies = Lazy.mk(this::calcDependencies);
  }

  static LinearJoinTree linearize(PlanContext plan, int treeRoot) {
    final InfoCache infoCache = plan.infoCache();

    int depth = 0, cursor = treeRoot;
    while (plan.kindOf(treeRoot) == PlanKind.Join && infoCache.isEquiJoin(cursor)) {
      ++depth;
      cursor = plan.childOf(cursor, 0);
    }

    if (depth == 0) return null;

    final int[] joiners = new int[depth], joinees = new int[depth + 1];
    cursor = treeRoot;
    while (plan.kindOf(treeRoot) == PlanKind.Join && infoCache.isEquiJoin(cursor)) {
      --depth;
      joiners[depth] = cursor;
      joinees[depth + 1] = plan.childOf(cursor, 1);
    }
    assert depth == 0;
    joinees[0] = plan.childOf(cursor, 0);

    return new LinearJoinTree(plan, joiners, joinees);
  }

  int numJoiners() {
    return joiners.length;
  }

  int joinerAt(int joinerIdx) {
    return joiners[joinerIdx];
  }

  int joineeAt(int joineeIdx) {
    return joinees[joineeIdx + 1];
  }

  int joinerOf(int joineeIdx) {
    return joinees[max(0, joineeIdx)];
  }

  int rootJoiner() {
    return joiners[joiners.length - 1];
  }

  boolean isEligibleRoot(int joineeIndex) {
    if (joineeIndex == -1) return ((JoinNode) plan.nodeAt(joiners[0])).joinKind() != LEFT_JOIN;
    if (joineeIndex == joinees.length - 1) return true;

    final int[] dependencies = this.dependencies.get();
    return ArraySupport.linearFind(dependencies, joineeIndex, joineeIndex + 2) == -1;
  }

  PlanContext mkRootedBy(int joineeIdx) {
    if (joineeIdx >= joinees.length - 2) return plan;

    final PlanContext newPlan = plan.copy();
    final int oldRootJoiner = rootJoiner();
    final int grandParent = newPlan.parentOf(oldRootJoiner);
    final int treeIndex = indexOfChild(newPlan, oldRootJoiner);
    final int newRootIdx = max(0, joineeIdx);
    final int newRootJoiner = joiners[newRootIdx];
    final int parent = newPlan.parentOf(newRootJoiner);
    final int child0 = newPlan.childOf(newRootJoiner, 0);
    final int child1 = newPlan.childOf(newRootJoiner, 1);

    if (joineeIdx >= 0) {
      assert newRootJoiner != oldRootJoiner;
      newPlan.detachNode(oldRootJoiner);
      newPlan.detachNode(newRootJoiner);
      newPlan.detachNode(child0);
      newPlan.setChild(grandParent, treeIndex, newRootJoiner);
      newPlan.setChild(newRootJoiner, 0, oldRootJoiner);
      newPlan.setChild(parent, 0, child0);

    } else {
      assert joineeIdx == -1;
      assert newRootJoiner == oldRootJoiner || parent != grandParent;

      newPlan.detachNode(newRootJoiner);
      newPlan.detachNode(child0);
      newPlan.detachNode(child1);
      newPlan.setChild(grandParent, treeIndex, newRootJoiner);
      newPlan.setChild(newRootJoiner, 1, child0);
      if (newRootJoiner == oldRootJoiner) newPlan.setChild(newRootJoiner, 0, child1);
      else {
        newPlan.detachNode(oldRootJoiner);
        newPlan.setChild(newRootJoiner, 0, oldRootJoiner);
        newPlan.setChild(parent, 0, child1);
      }

      final InfoCache infoCache = newPlan.infoCache();
      final var keys = infoCache.joinKeyOf(newRootJoiner);
      infoCache.setJoinKeyOf(newRootJoiner, keys.getRight(), keys.getLeft());
    }

    return newPlan;
  }

  private int[] calcDependencies() {
    final InfoCache infoCache = plan.infoCache();
    final int[] dependencies = new int[joinees.length];
    final ValuesRegistry valuesReg = plan.valuesReg();

    dependencies[0] = -1;
    dependencies[1] = 0;
    for (int i = 2, bound = joinees.length; i < bound; ++i) {
      final List<Value> lhsKeys = infoCache.lhsJoinKeyOf(joiners[i - 1]);
      for (int j = i - 1; j >= 0; --j)
        if (any(lhsKeys, valuesReg.valuesOf(joinees[j])::contains)) {
          dependencies[i] = j;
          break;
        }
    }

    return dependencies;
  }
}
