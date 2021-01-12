package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.operator.Sort;

public class SortImpl extends BaseOperator implements Sort {
  private SortImpl() {}

  public static SortImpl create() {
    return new SortImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public boolean accept0(GraphVisitor visitor) {
    return visitor.enterSort(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveSort(this);
  }
}
