package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;
import sjtu.ipads.wtune.superopt.plan.Sort;

public class SortImpl extends BasePlanNode implements Sort {
  private SortImpl() {}

  public static SortImpl create() {
    return new SortImpl();
  }

  @Override
  protected PlanNode newInstance() {
    return create();
  }

  @Override
  public boolean accept0(PlanVisitor visitor) {
    return visitor.enterSort(this);
  }

  @Override
  public void leave0(PlanVisitor visitor) {
    visitor.leaveSort(this);
  }
}
