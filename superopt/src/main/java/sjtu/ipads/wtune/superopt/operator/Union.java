package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.operator.impl.UnionImpl;

public interface Union extends Operator {
  @Override
  default OperatorType type() {
    return OperatorType.Union;
  }

  static Union create() {
    return UnionImpl.create();
  }
}
