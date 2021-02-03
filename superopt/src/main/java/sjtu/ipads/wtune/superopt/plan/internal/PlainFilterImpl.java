package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.PlainFilter;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public class PlainFilterImpl extends BasePlanNode implements PlainFilter {
  private final Placeholder fields;
  private final Placeholder predicate;

  private PlainFilterImpl() {
    fields = newPlaceholder("c");
    predicate = newPlaceholder("p");
  }

  public static PlainFilterImpl create() {
    return new PlainFilterImpl();
  }

  @Override
  public void setPlaceholders(String[] str) {
    fields.setIndex(Integer.parseInt(str[1].substring(str[1].indexOf('c') + 1)));
    predicate.setIndex(Integer.parseInt(str[2].substring(str[2].indexOf('p') + 1)));
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
