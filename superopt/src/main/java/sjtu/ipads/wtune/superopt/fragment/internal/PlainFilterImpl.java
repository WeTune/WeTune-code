package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;
import sjtu.ipads.wtune.superopt.fragment.PlainFilter;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public class PlainFilterImpl extends BaseOperator implements PlainFilter {
  private PlainFilterImpl() {}

  public static PlainFilterImpl create() {
    return new PlainFilterImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public Placeholder fields() {
    return plan().placeholders().getPick(this, 0);
  }

  @Override
  public Placeholder predicate() {
    return plan().placeholders().getPredicate(this, 0);
  }

  @Override
  public boolean accept0(OperatorVisitor visitor) {
    return visitor.enterPlainFilter(this);
  }

  @Override
  public void leave0(OperatorVisitor visitor) {
    visitor.leavePlainFilter(this);
  }
}
