package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.InnerJoin;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;

public class InnerJoinImpl extends JoinImpl implements InnerJoin {
  private InnerJoinImpl() {}

  public static InnerJoin create() {
    return new InnerJoinImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public boolean accept0(OperatorVisitor visitor) {
    return visitor.enterInnerJoin(this);
  }

  @Override
  public void leave0(OperatorVisitor visitor) {
    visitor.leaveInnerJoin(this);
  }
}
