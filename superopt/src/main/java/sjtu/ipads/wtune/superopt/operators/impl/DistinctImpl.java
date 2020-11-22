package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Distinct;

public class DistinctImpl extends BaseOperator implements Distinct {
  public static DistinctImpl create() {
    return new DistinctImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterDistinct(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveDistinct(this);
  }
  @Override public String toString() {
    return "Distinct";
  }
}
