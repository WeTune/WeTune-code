package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;
import sjtu.ipads.wtune.superopt.fragment.Sort;

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
  public boolean accept0(OperatorVisitor visitor) {
    return visitor.enterSort(this);
  }

  @Override
  public void leave0(OperatorVisitor visitor) {
    visitor.leaveSort(this);
  }
}
