package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Input;
import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public class InputImpl extends BasePlanNode implements Input {
  private InputImpl() {}

  public static InputImpl create() {
    return new InputImpl();
  }

  @Override
  protected PlanNode newInstance() {
    return this;
  }

  @Override
  public Placeholder table() {
    return plan().placeholders().getTable(this, 0);
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
