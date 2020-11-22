package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Union;

public class UnionImpl extends BaseOperator implements Union {
  protected UnionImpl() {
    super(2);
  }

  public static UnionImpl create() {
    return new UnionImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterUnion(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveUnion(this);
  }

  @Override
  public String toString() {
    return "Union";
  }
}
