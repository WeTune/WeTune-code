package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Distinct;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public class DistinctImpl extends BasePlanNode implements Distinct {
  private DistinctImpl() {}

  public static DistinctImpl create() {
    return new DistinctImpl();
  }

  @Override
  protected PlanNode newInstance() {
    return create();
  }

  @Override
  public boolean accept0(PlanVisitor visitor) {
    return visitor.enterDistinct(this);
  }

  @Override
  public void leave0(PlanVisitor visitor) {
    visitor.leaveDistinct(this);
  }
}
