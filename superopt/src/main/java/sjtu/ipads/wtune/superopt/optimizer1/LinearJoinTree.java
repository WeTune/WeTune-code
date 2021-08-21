package sjtu.ipads.wtune.superopt.optimizer1;

import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.elemAt;

public interface LinearJoinTree {
  List<JoinNode> joiners();

  List<PlanNode> joinees();

  boolean isEligibleRoot(int joineeIndex);

  JoinNode mkRootedByJoinee(int joineeIndex);

  default JoinNode rootJoiner() {
    return elemAt(joiners(), -1);
  }
}
