package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.InnerJoin;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public class InnerJoinImpl extends JoinImpl implements InnerJoin {
  private InnerJoinImpl() {}

  public static InnerJoin create() {
    return new InnerJoinImpl();
  }

  @Override
  protected PlanNode newInstance() {
    return create();
  }

  @Override
  public boolean accept0(PlanVisitor visitor) {
    return visitor.enterInnerJoin(this);
  }

  @Override
  public void leave0(PlanVisitor visitor) {
    visitor.leaveInnerJoin(this);
  }
}
