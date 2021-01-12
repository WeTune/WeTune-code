package sjtu.ipads.wtune.superopt.operator;

import sjtu.ipads.wtune.superopt.operator.impl.SortImpl;

public interface Sort extends Operator {
  @Override
  default OperatorType type() {
    return OperatorType.Sort;
  }

  static Sort create() {
    return SortImpl.create();
  }
}
