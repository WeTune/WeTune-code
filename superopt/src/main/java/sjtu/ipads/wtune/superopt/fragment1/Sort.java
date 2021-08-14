package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface Sort extends Op {
  @Override
  default OperatorType kind() {
    return OperatorType.SORT;
  }
}
