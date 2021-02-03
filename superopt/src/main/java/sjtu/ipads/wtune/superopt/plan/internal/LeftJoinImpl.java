package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.LeftJoin;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public class LeftJoinImpl extends JoinImpl implements LeftJoin {
  private LeftJoinImpl() {}

  public static LeftJoin create() {
    return new LeftJoinImpl();
  }

  @Override
  protected PlanNode newInstance() {
    return create();
  }

  @Override
  public boolean accept0(PlanVisitor visitor) {
    return visitor.enterLeftJoin(this);
  }

  @Override
  public void leave0(PlanVisitor visitor) {
    visitor.leaveLeftJoin(this);
  }
}
