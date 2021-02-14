package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.plan.internal.PlainFilterImpl;

public interface PlainFilter extends PlanNode {
  Placeholder fields();

  Placeholder predicate();

  @Override
  default OperatorType type() {
    return OperatorType.PlainFilter;
  }

  static PlainFilter create() {
    return PlainFilterImpl.create();
  }
}
