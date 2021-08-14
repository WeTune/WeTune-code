package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.internal.UnionImpl;

public interface Union extends Operator {
  @Override
  default OperatorType kind() {
    return OperatorType.UNION;
  }

  static Union create() {
    return UnionImpl.create();
  }
}
