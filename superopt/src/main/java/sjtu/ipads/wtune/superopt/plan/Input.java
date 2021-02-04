package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.internal.InputImpl;
import sjtu.ipads.wtune.superopt.plan.symbolic.Placeholder;

public interface Input extends PlanNode {
  static Input create() {
    return InputImpl.create();
  }

  Placeholder table();

  @Override
  default OperatorType type() {
    return OperatorType.Input;
  }
}
