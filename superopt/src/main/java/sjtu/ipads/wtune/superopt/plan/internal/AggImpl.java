package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Agg;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public class AggImpl extends BasePlanNode implements Agg {
  private AggImpl() {}

  public static AggImpl create() {
    return new AggImpl();
  }

  @Override
  protected PlanNode newInstance() {
    return create();
  }

  @Override
  public boolean accept0(PlanVisitor visitor) {
    return visitor.enterAgg(this);
  }

  @Override
  public void leave0(PlanVisitor visitor) {
    visitor.leaveAgg(this);
  }
}
