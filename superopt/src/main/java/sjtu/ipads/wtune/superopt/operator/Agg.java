package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.operator.impl.AggImpl;

public interface Agg extends Operator {
  static Agg create() {
    return AggImpl.create();
  }

  @Override
  default OperatorType type() {
    return OperatorType.Agg;
  }
}
