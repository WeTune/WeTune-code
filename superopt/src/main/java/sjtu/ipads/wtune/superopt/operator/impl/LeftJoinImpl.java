package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.LeftJoin;
import sjtu.ipads.wtune.superopt.operator.Operator;

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
  public boolean accept0(GraphVisitor visitor) {
    return visitor.enterLeftJoin(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveLeftJoin(this);
  }
}
