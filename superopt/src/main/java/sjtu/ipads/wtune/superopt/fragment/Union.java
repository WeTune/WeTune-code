package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.internal.UnionImpl;

public interface Union extends Operator {
  @Override
  default OperatorType type() {
    return OperatorType.Union;
  }

  static Union create() {
    return UnionImpl.create();
  }
}
