package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.internal.InnerJoinImpl;

public interface InnerJoin extends Join {

  @Override
  default OperatorType type() {
    return OperatorType.InnerJoin;
  }

  static InnerJoin create() {
    return InnerJoinImpl.create();
  }
}
