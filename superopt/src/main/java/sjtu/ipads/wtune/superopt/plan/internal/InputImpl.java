package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Input;
import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public class InputImpl extends BasePlanNode implements Input {
  private final Placeholder table;

  private InputImpl() {
    this.table = newPlaceholder("t");
  }

  public static InputImpl create() {
    return new InputImpl();
  }

  @Override
  public void setPlaceholders(String[] str) {
    table.setIndex(Integer.parseInt(str[1].substring(str[1].indexOf('t') + 1)));
  }

  @Override
  protected PlanNode newInstance() {
    return this;
  }

  @Override
  public Placeholder table() {
    return table;
  }

  @Override
  public boolean accept0(PlanVisitor visitor) {
    return visitor.enterInput(this);
  }

  @Override
  public void leave0(PlanVisitor visitor) {
    visitor.leaveInput(this);
  }
}
