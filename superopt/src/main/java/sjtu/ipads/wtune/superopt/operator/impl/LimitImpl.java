package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.operator.Limit;
import sjtu.ipads.wtune.superopt.operator.Operator;

public class LimitImpl extends BaseOperator implements Limit {
  private LimitImpl() {}

  public static LimitImpl create() {
    return new LimitImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public boolean accept0(GraphVisitor visitor) {
    return visitor.enterLimit(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveLimit(this);
  }
}
