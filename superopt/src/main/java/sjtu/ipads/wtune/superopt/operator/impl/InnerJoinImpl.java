package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.operator.InnerJoin;
import sjtu.ipads.wtune.superopt.operator.Operator;

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
  public boolean accept0(GraphVisitor visitor) {
    return visitor.enterInnerJoin(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveInnerJoin(this);
  }
}
