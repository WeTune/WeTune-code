package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Join;
import sjtu.ipads.wtune.superopt.operators.PlainFilter;
import sjtu.ipads.wtune.superopt.operators.SubqueryFilter;

public class SubqueryFilterImpl extends BaseOperator implements SubqueryFilter {
  protected SubqueryFilterImpl() {
    super(2);
  }

  public static SubqueryFilterImpl create() {
    return new SubqueryFilterImpl();
  }

  @Override
  public boolean setPrev(int idx, Operator prev) {
    if (idx == 0) return super.setPrev(idx, prev);
    if (prev instanceof Join || prev instanceof PlainFilter || prev instanceof SubqueryFilter)
      return false;
    else return super.setPrev(idx, prev);
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterSubqueryFilter(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveSubqueryFilter(this);
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public String toString() {
    return "Filter*";
  }
}
