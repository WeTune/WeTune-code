package sjtu.ipads.wtune.superopt.optimizer1;

import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;

public interface LinearJoinTree {
  JoinNode rootJoiner();

  boolean isEligibleRoot(int joineeIndex);

  JoinNode mkRootedBy(int joineeIndex);
}
