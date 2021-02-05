package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.PlainFilter;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public class PlainFilterImpl extends BasePlanNode implements PlainFilter {
  private final Placeholder fields;
  private final Placeholder predicate;

  private PlainFilterImpl() {
    fields = makePlaceholder("c");
    predicate = makePlaceholder("p");
  }

  public static PlainFilterImpl create() {
    return new PlainFilterImpl();
  }

  @Override
  protected PlanNode newInstance() {
    return create();
  }

  @Override
  public Placeholder fields() {
    return fields;
  }

  @Override
  public Placeholder predicate() {
    return predicate;
  }

  @Override
  public boolean accept0(PlanVisitor visitor) {
    return visitor.enterPlainFilter(this);
  }

  @Override
  public void leave0(PlanVisitor visitor) {
    visitor.leavePlainFilter(this);
  }
}
