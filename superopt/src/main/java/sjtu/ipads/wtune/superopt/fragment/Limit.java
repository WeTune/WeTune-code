package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.internal.LimitImpl;

public interface Limit extends Operator {
  static Limit create() {
    return LimitImpl.create();
  }

  @Override
  default OperatorType type() {
    return OperatorType.LIMIT;
  }
}
