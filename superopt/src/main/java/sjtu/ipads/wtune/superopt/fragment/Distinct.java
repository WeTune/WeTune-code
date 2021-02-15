package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.internal.DistinctImpl;

public interface Distinct extends Operator {
  static Distinct create() {
    return DistinctImpl.create();
  }

  @Override
  default OperatorType type() {
    return OperatorType.Distinct;
  }
}
