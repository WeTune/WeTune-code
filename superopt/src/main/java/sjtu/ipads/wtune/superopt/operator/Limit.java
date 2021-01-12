package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.operator.impl.LimitImpl;

public interface Limit extends Operator {
  static Limit create() {
    return LimitImpl.create();
  }

  @Override
  default OperatorType type() {
    return OperatorType.Limit;
  }
}
