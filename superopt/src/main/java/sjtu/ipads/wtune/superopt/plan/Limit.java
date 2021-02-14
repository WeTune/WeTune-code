package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.plan.internal.LimitImpl;

public interface Limit extends PlanNode {
  static Limit create() {
    return LimitImpl.create();
  }

  @Override
  default OperatorType type() {
    return OperatorType.Limit;
  }
}
