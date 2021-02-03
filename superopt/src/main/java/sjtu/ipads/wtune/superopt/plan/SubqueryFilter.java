package sjtu.ipads.wtune.superopt.plan;

import sjtu.ipads.wtune.superopt.plan.internal.SubqueryFilterImpl;

public interface SubqueryFilter extends PlanNode {
  Placeholder fields();

  @Override
  default OperatorType type() {
    return OperatorType.SubqueryFilter;
  }

  static SubqueryFilter create() {
    return SubqueryFilterImpl.create();
  }
}
