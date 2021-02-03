package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.internal.LeftJoinImpl;

public interface LeftJoin extends Join {

  static LeftJoin create() {
    return LeftJoinImpl.create();
  }

  @Override
  default OperatorType type() {
    return OperatorType.LeftJoin;
  }
}
