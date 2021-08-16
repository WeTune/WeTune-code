package sjtu.ipads.wtune.superopt.optimizer1;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.common.utils.TreeScaffold;
import sjtu.ipads.wtune.common.utils.TreeTemplate;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.max;
import static sjtu.ipads.wtune.common.utils.Commons.elemAt;
import static sjtu.ipads.wtune.common.utils.FuncUtils.any;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.copyPlan;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.planRootOf;

class JoinTreeImpl {
  private final List<JoinNode> joiners;
  private final List<PlanNode> joinees;

  private int[] dependencies;

  JoinTreeImpl(List<JoinNode> joiners, List<PlanNode> joinees) {
    this.joiners = joiners;
    this.joinees = joinees;
  }

  static JoinTreeImpl mk(JoinNode root) {
    final List<JoinNode> joiners = new ArrayList<>(6);
    final List<PlanNode> joinees = new ArrayList<>(7);

    linearize(root, joiners, joinees);
    return new JoinTreeImpl(Lists.reverse(joiners), Lists.reverse(joinees));
  }

  static void linearize(JoinNode joiner, List<JoinNode> joiners, List<PlanNode> joinees) {
    joiners.add(joiner);
    joinees.add(joiner.predecessors()[1]);

    final PlanNode lhs = joiner.predecessors()[0];
    if (lhs.kind() == OperatorType.INNER_JOIN) {
      linearize((JoinNode) lhs, joiners, joinees);
    } else {
      joinees.add(lhs);
    }
  }

  JoinNode rootJoiner() {
    return elemAt(joiners, -1);
  }

  boolean canBeRoot(int joineeIndex) {
    final int[] dependencies = dependencies();
    // Consider the leftmost joiner, we can always flip it.
    // e.g., A Join B On p(A,B) Join C On P(B,C), dependencies=[-1,0,1]
    // we can always safely turn it to `B Join A On p(A,B) Join C On P(B,C)`,
    // and dependencies=[-1,0,0].
    // In other words, the `dependencies[0]` is always 0, no matter which is the leftmost joinee.
    for (int i = max(2, joineeIndex + 1), bound = joinees.size(); i < bound; ++i) {
      if (dependencies[i] == joineeIndex) return false;
    }
    return true;
  }

  JoinNode rebuildWithRoot(int joineeIndex) {
    final JoinNode oldJoinRoot = rootJoiner();
    final PlanNode planRoot = planRootOf(oldJoinRoot);
    final PlanContext newContext = PlanContext.mk(planRoot.context().schema());

    final TreeScaffold<PlanNode> scaffold = new TreeScaffold<>(planRoot, it -> it.copy(newContext));
    final TreeTemplate<PlanNode> base = scaffold.rootTemplate();

    final List<JoinNode> joiners;

    if (joineeIndex > 0) joiners = this.joiners;
    else {
      joiners = new ArrayList<>(this.joiners);

      final PlanNode newSnd = copyPlan(joinees.get(0), newContext);
      final PlanNode newFst = copyPlan(joinees.get(1), newContext);
      final JoinNode flipped = joiners.get(0).flip(newContext);

      flipped.setPredecessor(0, newFst);
      flipped.setPredecessor(1, newSnd);
      joiners.set(0, flipped);
    }

    final JoinNode newRootJoiner = joiners.get(max(0, joineeIndex - 1));

    final TreeTemplate<PlanNode> joinTemplate = base.bindJointPoint(oldJoinRoot, newRootJoiner);

    TreeTemplate<PlanNode> succ = joinTemplate;
    for (int joinerIndex = joiners.size() - 1; joinerIndex >= 0; --joinerIndex)
      if (joinerIndex != joineeIndex - 1) {
        succ = succ.bindJointPoint(succ.root().predecessors()[0], joiners.get(joinerIndex));
      }

    scaffold.instantiate();
    return (JoinNode) joinTemplate.getInstantiated();
  }

  private int[] dependencies() {
    if (dependencies != null) return dependencies;

    final int[] dependencies = this.dependencies = new int[joinees.size()];

    for (int i = 1, bound = joinees.size(); i < bound; ++i) {
      final JoinNode joiner = joiners.get(i - 1);
      final RefBag joinKeys = joiner.isEquiJoin() ? joiner.lhsRefs() : joiner.refs();
      final List<Value> values = joiner.context().deRef(joinKeys);

      for (int j = i - 1; j >= 0; --j) {
        final PlanNode prevJoinee = joinees.get(i);
        if (any(values, prevJoinee.values()::contains)) {
          dependencies[i] = j;
          break;
        }
      }
    }

    return dependencies;
  }
}
