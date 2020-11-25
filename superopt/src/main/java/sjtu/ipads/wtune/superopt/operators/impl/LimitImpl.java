package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Limit;

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
  public void accept0(GraphVisitor visitor) {
    visitor.enterLimit(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveLimit(this);
  }

  @Override
  public String toString() {
    return "Limit" + id();
  }
}
