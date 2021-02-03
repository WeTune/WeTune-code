package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;
import sjtu.ipads.wtune.superopt.plan.Union;

public class UnionImpl extends BasePlanNode implements Union {
  private UnionImpl() {}

  public static UnionImpl create() {
    return new UnionImpl();
  }

  @Override
  protected PlanNode newInstance() {
    return create();
  }

  @Override
  public boolean accept0(PlanVisitor visitor) {
    return visitor.enterUnion(this);
  }

  @Override
  public void leave0(PlanVisitor visitor) {
    visitor.leaveUnion(this);
  }
}
