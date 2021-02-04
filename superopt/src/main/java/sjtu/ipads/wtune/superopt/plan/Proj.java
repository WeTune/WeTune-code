package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.internal.ProjImpl;
import sjtu.ipads.wtune.superopt.plan.symbolic.Placeholder;

public interface Proj extends PlanNode {
  Placeholder fields();

  @Override
  default OperatorType type() {
    return OperatorType.Proj;
  }

  static Proj create() {
    return ProjImpl.create();
  }
}
