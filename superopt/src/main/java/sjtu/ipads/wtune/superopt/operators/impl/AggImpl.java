package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.Agg;

public class AggImpl extends BaseOperator implements Agg {
  public AggImpl() {}

  public static AggImpl create() {
    return new AggImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterAgg(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveAgg(this);
  }

  @Override
  public String toString() {
    return "Agg";
  }
}
