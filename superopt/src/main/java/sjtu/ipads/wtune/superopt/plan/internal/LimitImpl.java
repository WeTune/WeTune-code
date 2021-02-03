package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Limit;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public class LimitImpl extends BasePlanNode implements Limit {
  private LimitImpl() {}

  public static LimitImpl create() {
    return new LimitImpl();
  }

  @Override
  protected PlanNode newInstance() {
    return create();
  }

  @Override
  public boolean accept0(PlanVisitor visitor) {
    return visitor.enterLimit(this);
  }

  @Override
  public void leave0(PlanVisitor visitor) {
    visitor.leaveLimit(this);
  }
}
