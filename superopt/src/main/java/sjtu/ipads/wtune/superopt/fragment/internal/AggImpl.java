package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.Agg;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;

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
  public boolean accept0(OperatorVisitor visitor) {
    return visitor.enterAgg(this);
  }

  @Override
  public void leave0(OperatorVisitor visitor) {
    visitor.leaveAgg(this);
  }
}
