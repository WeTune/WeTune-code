package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.internal.DistinctImpl;

public interface Distinct extends PlanNode {
  static Distinct create() {
    return DistinctImpl.create();
  }

  @Override
  default OperatorType type() {
    return OperatorType.Distinct;
  }
}
