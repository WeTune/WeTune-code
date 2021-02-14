package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.plan.internal.SortImpl;

public interface Sort extends PlanNode {
  @Override
  default OperatorType type() {
    return OperatorType.Sort;
  }

  static Sort create() {
    return SortImpl.create();
  }
}
