package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;
import sjtu.ipads.wtune.superopt.plan.Proj;

public class ProjImpl extends BasePlanNode implements Proj {
  private final Placeholder fields;

  private ProjImpl() {
    fields = newPlaceholder("c");
  }

  public static ProjImpl create() {
    return new ProjImpl();
  }

  @Override
  public void setPlaceholders(String[] str) {
    fields.setIndex(Integer.parseInt(str[1].substring(str[1].indexOf('c') + 1)));
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
  public boolean accept0(PlanVisitor visitor) {
    return visitor.enterProj(this);
  }

  @Override
  public void leave0(PlanVisitor visitor) {
    visitor.leaveProj(this);
  }
}
