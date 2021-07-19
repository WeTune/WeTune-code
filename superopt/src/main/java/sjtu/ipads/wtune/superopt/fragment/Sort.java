package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.internal.SortImpl;

public interface Sort extends Operator {
  @Override
  default OperatorType type() {
    return OperatorType.SORT;
  }

  static Sort create() {
    return SortImpl.create();
  }
}
