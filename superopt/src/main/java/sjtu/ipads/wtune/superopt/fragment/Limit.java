package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface Limit extends Op {
  @Override
  default OperatorType kind() {
    return OperatorType.LIMIT;
  }
}
