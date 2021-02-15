package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;
import sjtu.ipads.wtune.superopt.fragment.SubqueryFilter;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public class SubqueryFilterImpl extends BaseOperator implements SubqueryFilter {
  private SubqueryFilterImpl() {}

  public static SubqueryFilterImpl create() {
    return new SubqueryFilterImpl();
  }

  @Override
  public Placeholder fields() {
    return plan().placeholders().getPick(this, 0);
  }

  @Override
  public boolean accept0(OperatorVisitor visitor) {
    return visitor.enterSubqueryFilter(this);
  }

  @Override
  public void leave0(OperatorVisitor visitor) {
    visitor.leaveSubqueryFilter(this);
  }

  @Override
  protected Operator newInstance() {
    return create();
  }
}
