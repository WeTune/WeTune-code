package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.plan.internal.UnionImpl;

public interface Union extends PlanNode {
  @Override
  default OperatorType type() {
    return OperatorType.Union;
  }

  static Union create() {
    return UnionImpl.create();
  }
}
