package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface InnerJoin extends Join {
  @Override
  default OperatorType type() {
    return OperatorType.INNER_JOIN;
  }
}
