package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;
import sjtu.ipads.wtune.superopt.plan.SubqueryFilter;

public class SubqueryFilterImpl extends BasePlanNode implements SubqueryFilter {
  private final Placeholder fields;

  private SubqueryFilterImpl() {
    fields = newPlaceholder("c");
  }

  public static SubqueryFilterImpl create() {
    return new SubqueryFilterImpl();
  }

  @Override
  public void setPlaceholders(String[] str) {
    fields.setIndex(Integer.parseInt(str[1].substring(str[1].indexOf('c') + 1)));
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
