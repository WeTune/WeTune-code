package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.operator.Agg;
import sjtu.ipads.wtune.superopt.operator.Operator;

public class AggImpl extends BaseOperator implements Agg {
  private AggImpl() {}

  public static AggImpl create() {
    return new AggImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public boolean accept0(GraphVisitor visitor) {
    return visitor.enterAgg(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveAgg(this);
  }
}
