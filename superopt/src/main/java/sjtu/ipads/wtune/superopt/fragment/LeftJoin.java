package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.internal.LeftJoinImpl;

public interface LeftJoin extends Join {

  static LeftJoin create() {
    return LeftJoinImpl.create();
  }

  @Override
  default OperatorType type() {
    return OperatorType.LEFT_JOIN;
  }
}
