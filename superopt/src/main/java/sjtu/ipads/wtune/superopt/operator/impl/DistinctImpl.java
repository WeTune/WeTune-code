package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.operator.Distinct;
import sjtu.ipads.wtune.superopt.operator.Operator;

public class DistinctImpl extends BaseOperator implements Distinct {
  private DistinctImpl() {}

  public static DistinctImpl create() {
    return new DistinctImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public boolean accept0(GraphVisitor visitor) {
    return visitor.enterDistinct(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveDistinct(this);
  }
}
