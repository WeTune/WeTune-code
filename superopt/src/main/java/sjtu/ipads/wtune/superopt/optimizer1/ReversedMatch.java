package sjtu.ipads.wtune.superopt.optimizer1;

import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.superopt.fragment1.ConstraintAwareModel;
import sjtu.ipads.wtune.superopt.fragment1.Join;
import sjtu.ipads.wtune.superopt.fragment1.Op;

import java.util.List;

public interface ReversedMatch<P extends PlanNode, O extends Op> {
  List<P> reverseMatch(P plan, O op, ConstraintAwareModel model);

  List<P> results();

  static ReversedMatch<JoinNode, Join> forJoin() {
    return new JoinReversedMatch();
  }
}
