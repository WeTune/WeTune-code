package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface Union extends Op {
  @Override
  default OperatorType kind() {
    return OperatorType.UNION;
  }
}
