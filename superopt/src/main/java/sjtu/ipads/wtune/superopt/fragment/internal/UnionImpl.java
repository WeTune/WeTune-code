package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;
import sjtu.ipads.wtune.superopt.fragment.Union;

public class UnionImpl extends BaseOperator implements Union {
  private UnionImpl() {}

  public static UnionImpl create() {
    return new UnionImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public boolean accept0(OperatorVisitor visitor) {
    return visitor.enterUnion(this);
  }

  @Override
  public void leave0(OperatorVisitor visitor) {
    visitor.leaveUnion(this);
  }
}
