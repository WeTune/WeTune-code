package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.internal.AggImpl;

public interface Agg extends Operator {
  static Agg create() {
    return AggImpl.create();
  }

  @Override
  default OperatorType kind() {
    return OperatorType.AGG;
  }
}
