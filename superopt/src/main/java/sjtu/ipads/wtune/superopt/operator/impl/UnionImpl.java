package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.operator.Union;

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
  public boolean accept0(GraphVisitor visitor) {
    return visitor.enterUnion(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveUnion(this);
  }
}
