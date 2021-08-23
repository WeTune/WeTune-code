package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface LeftJoin extends Join {
  @Override
  default OperatorType kind() {
    return OperatorType.LEFT_JOIN;
  }
}