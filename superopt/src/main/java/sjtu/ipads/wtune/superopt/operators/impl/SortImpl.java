package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.Sort;

public class SortImpl extends BaseOperator implements Sort {
  public static SortImpl create() {
    return new SortImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterSort(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveSort(this);
  }

  @Override
  public String toString() {
    return "Sort";
  }
}
