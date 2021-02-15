package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.Limit;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;

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
  public boolean accept0(OperatorVisitor visitor) {
    return visitor.enterLimit(this);
  }

  @Override
  public void leave0(OperatorVisitor visitor) {
    visitor.leaveLimit(this);
  }
}
