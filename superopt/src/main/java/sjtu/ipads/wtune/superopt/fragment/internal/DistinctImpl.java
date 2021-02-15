package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.Distinct;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;

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
  public boolean accept0(OperatorVisitor visitor) {
    return visitor.enterDistinct(this);
  }

  @Override
  public void leave0(OperatorVisitor visitor) {
    visitor.leaveDistinct(this);
  }
}
