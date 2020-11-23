package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.Join;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.PlainFilter;
import sjtu.ipads.wtune.superopt.operators.SubqueryFilter;
import sjtu.ipads.wtune.superopt.relational.SubqueryPredicate;

public class SubqueryFilterImpl extends BaseOperator implements SubqueryFilter {
  private final Abstraction<SubqueryPredicate> predicate =
      Abstraction.create(this, "filter*-" + id());

  protected SubqueryFilterImpl() {
    super(2);
  }

  @Override
  public Abstraction<SubqueryPredicate> predicate() {
    return predicate;
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
