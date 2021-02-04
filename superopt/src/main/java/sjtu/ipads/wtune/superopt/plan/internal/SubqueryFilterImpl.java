package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;
import sjtu.ipads.wtune.superopt.plan.SubqueryFilter;

public class SubqueryFilterImpl extends BasePlanNode implements SubqueryFilter {
  private final Placeholder fields;

  private SubqueryFilterImpl() {
    fields = makePlaceholder("c");
  }

  public static SubqueryFilterImpl create() {
    return new SubqueryFilterImpl();
  }

  @Override
  public Placeholder fields() {
    return fields;
  }

  @Override
  public boolean accept0(PlanVisitor visitor) {
    return visitor.enterSubqueryFilter(this);
  }

  @Override
  public void leave0(PlanVisitor visitor) {
    visitor.leaveSubqueryFilter(this);
  }

  @Override
  protected PlanNode newInstance() {
    return create();
  }
}
