package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface Agg extends Op {
  @Override
  default OperatorType type() {
    return OperatorType.AGG;
  }
}
