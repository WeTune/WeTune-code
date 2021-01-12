package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.operator.impl.DistinctImpl;

public interface Distinct extends Operator {
  static Distinct create() {
    return DistinctImpl.create();
  }

  @Override
  default OperatorType type() {
    return OperatorType.Distinct;
  }
}
