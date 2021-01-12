package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.operator.impl.LeftJoinImpl;

public interface LeftJoin extends Join {

  static LeftJoin create() {
    return LeftJoinImpl.create();
  }

  @Override
  default OperatorType type() {
    return OperatorType.LeftJoin;
  }
}
