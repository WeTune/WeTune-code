package sjtu.ipads.wtune.superopt.optimizer1;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.common.utils.TreeScaffold;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.max;
import static sjtu.ipads.wtune.common.utils.Commons.elemAt;
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
  public JoinNode rootJoiner() {
    return elemAt(joiners, -1);
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

    for (int i = joineeIndex, bound = joiners.size(); i < bound; ++i) {
      if (joiners.get(i).kind() == OperatorType.LEFT_JOIN) return false;
    }

    return true;
  }

  @Override
  public JoinNode mkRootedBy(int joineeIndex) {
    if (joineeIndex == joinees.size() - 1) return joiners.get(joiners.size() - 1);

    final JoinNode oldJoinRoot = rootJoiner();
    final PlanContext newCtx = oldJoinRoot.context().dup();

    final List<JoinNode> joiners;
    if (joineeIndex > 0) joiners = this.joiners;
    else {
      joiners = new ArrayList<>(this.joiners);
      final PlanNode newFst = replaceLocal(newCtx, joiners.get(0), joinees.get(1), joinees.get(0));
      final PlanNode newSnd = replaceLocal(newCtx, joiners.get(1), joinees.get(1));
      joiners.set(0, ((JoinNode) newFst).flip(null));
      joiners.set(1, ((JoinNode) newSnd));
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
