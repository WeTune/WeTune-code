package sjtu.ipads.wtune.superopt.optimizer;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.common.utils.TreeScaffold;
import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.max;
import static sjtu.ipads.wtune.common.utils.Commons.listSwap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.any;
import static sjtu.ipads.wtune.common.utils.TreeScaffold.displaceGlobal;
import static sjtu.ipads.wtune.common.utils.TreeScaffold.replaceLocal;

class LinearJoinTreeImpl implements LinearJoinTree {
  private final List<JoinNode> joiners;
  private final List<PlanNode> joinees;

  private int[] dependencies;

  LinearJoinTreeImpl(List<JoinNode> joiners, List<PlanNode> joinees) {
    this.joiners = joiners;
    this.joinees = joinees;
  }

  static LinearJoinTreeImpl mk(JoinNode root) {
    final List<JoinNode> joiners = new ArrayList<>(6);
    final List<PlanNode> joinees = new ArrayList<>(7);

    linearize(root, joiners, joinees);
    return new LinearJoinTreeImpl(Lists.reverse(joiners), Lists.reverse(joinees));
  }

  static void linearize(JoinNode joiner, List<JoinNode> joiners, List<PlanNode> joinees) {
    joiners.add(joiner);
    joinees.add(joiner.predecessors()[1]);

    final PlanNode lhs = joiner.predecessors()[0];
    if (lhs.kind().isJoin()) {
      linearize((JoinNode) lhs, joiners, joinees);
    } else {
      joinees.add(lhs);
    }
  }

  @Override
  public List<JoinNode> joiners() {
    return joiners;
  }

  @Override
  public List<PlanNode> joinees() {
    return joinees;
  }

  @Override
  public boolean isEligibleRoot(int joineeIndex) {
    final int[] dependencies = dependencies();
    // Consider the leftmost joiner, we can always flip it.
    // e.g., A Join B On p(A,B) Join C On P(B,C), dependencies=[-1,0,1]
    // we can always safely turn it to `B Join A On p(A,B) Join C On P(B,C)`,
    // and dependencies=[-1,0,0].
    // In other words, the `dependencies[0]` is always 0, no matter which is the leftmost joinee.
    for (int i = max(2, joineeIndex + 1), bound = joinees.size(); i < bound; ++i) {
      if (dependencies[i] == joineeIndex) return false;
    }

    if (joineeIndex == 0 && joiners.get(0).kind() == OperatorType.LEFT_JOIN)
      for (int i = joineeIndex, bound = joiners.size(); i < bound; ++i) {
        if (joiners.get(i).kind() == OperatorType.LEFT_JOIN) return false;
      }

    return true;
  }

  @Override
  public JoinNode mkRootedByJoinee(int joineeIndex) {
    if (joineeIndex == joinees.size() - 1) return joiners.get(joiners.size() - 1);

    final JoinNode oldJoinRoot = rootJoiner();
    final PlanContext newCtx = oldJoinRoot.context().dup();

    final List<JoinNode> joiners;
    final List<PlanNode> joinees;
    if (joineeIndex > 0) {
      joiners = this.joiners;
      joinees = this.joinees;
    } else {
      joiners = new ArrayList<>(this.joiners);
      joinees = new ArrayList<>(this.joinees);
      final PlanNode newHead = replaceLocal(newCtx, joiners.get(0), joinees.get(1), joinees.get(0));
      joiners.set(0, ((JoinNode) newHead).flip(null));
      listSwap(joinees, 0, 1);
    }

    final int newRootJoinerIndex = max(0, joineeIndex - 1);
    final JoinNode newRootJoiner = joiners.get(newRootJoinerIndex);
    final var scaffold = new TreeScaffold<>(newRootJoiner, newCtx);

    var template = scaffold.rootTemplate();
    for (int joinerIndex = joiners.size() - 1; joinerIndex >= 0; --joinerIndex)
      if (joinerIndex != newRootJoinerIndex) {
        template =
            template.bindJointPoint(template.root().predecessors()[0], joiners.get(joinerIndex));
      }
    template.bindJointPoint(template.root().predecessors()[0], joinees.get(0));

    return ((JoinNode) displaceGlobal(oldJoinRoot, scaffold.instantiate(), false));
  }

  private int[] dependencies() {
    if (dependencies != null) return dependencies;

    final int[] dependencies = this.dependencies = new int[joinees.size()];

    for (int i = 1, bound = joinees.size(); i < bound; ++i) {
      final JoinNode joiner = joiners.get(i - 1);
      final RefBag joinKeys = joiner.isEquiJoin() ? joiner.lhsRefs() : joiner.refs();
      final List<Value> values = joiner.context().deRef(joinKeys);

      for (int j = i - 1; j >= 0; --j) {
        final PlanNode prevJoinee = joinees.get(j);
        if (any(values, prevJoinee.values()::contains)) {
          dependencies[i] = j;
          break;
        }
      }
    }

    return dependencies;
  }
}
