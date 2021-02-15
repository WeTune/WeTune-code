package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.LeftJoin;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;

public class LeftJoinImpl extends JoinImpl implements LeftJoin {
  private LeftJoinImpl() {}

  public static LeftJoin create() {
    return new LeftJoinImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public boolean accept0(OperatorVisitor visitor) {
    return visitor.enterLeftJoin(this);
  }

  @Override
  public void leave0(OperatorVisitor visitor) {
    visitor.leaveLeftJoin(this);
  }
}
