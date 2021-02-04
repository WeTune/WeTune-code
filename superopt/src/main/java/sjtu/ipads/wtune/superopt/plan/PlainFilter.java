package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.internal.PlainFilterImpl;
import sjtu.ipads.wtune.superopt.plan.symbolic.Placeholder;

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
