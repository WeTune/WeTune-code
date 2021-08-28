package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.ConstraintAwareModel;
import sjtu.ipads.wtune.superopt.fragment.Join;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.linearizeJoinTree;

class JoinReversedMatch implements ReversedMatch<JoinNode, Join> {
  private Join op;
  private LinearJoinTree linearJoinTree;
  private ConstraintAwareModel whatIf;
  private List<JoinNode> results;

  @Override
  public List<JoinNode> reverseMatch(JoinNode joinRoot, Join op, ConstraintAwareModel model) {
    this.op = op;
    this.linearJoinTree = linearizeJoinTree(joinRoot);
    this.whatIf = model.derive();

    final List<JoinNode> matched = new ArrayList<>(linearJoinTree.joinees().size());
    for (int i = linearJoinTree.joinees().size() - 1; i >= 0; i--)
      if (tryMatchAt(i)) {
        matched.add(linearJoinTree.mkRootedByJoinee(i));
      }

    return results = matched;
  }

  @Override
  public List<JoinNode> results() {
    return results;
  }

  private boolean tryMatchAt(int joineeIndex) {
    if (!linearJoinTree.isEligibleRoot(joineeIndex)) return false;

    final JoinNode joiner = linearJoinTree.joiners().get(Integer.max(0, joineeIndex - 1));
    final PlanNode joinee = linearJoinTree.joinees().get(joineeIndex);

    if (joineeIndex == 0) joiner.flip(null);

    whatIf.reset();
    boolean matched =
        op.match(joiner, whatIf)
            && op.predecessors()[1].match(joinee, whatIf)
            && whatIf.checkConstraint();

    if (joineeIndex == 0) joiner.flip(null);

    return matched;
  }
}
