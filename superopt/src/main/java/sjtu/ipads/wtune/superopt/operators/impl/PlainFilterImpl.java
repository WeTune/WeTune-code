package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.PlainFilter;

public class PlainFilterImpl extends BaseOperator implements PlainFilter {
  public static PlainFilterImpl create() {
    return new PlainFilterImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterPlainFilter(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leavePlainFilter(this);
  }

  @Override
  public String toString() {
    return "Filter";
  }
}
