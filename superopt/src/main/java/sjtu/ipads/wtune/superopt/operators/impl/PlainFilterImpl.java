package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.PlainFilter;
import sjtu.ipads.wtune.superopt.relational.PlainPredicate;

public class PlainFilterImpl extends BaseOperator implements PlainFilter {
  private final Abstraction<PlainPredicate> predicate = Abstraction.create(this, "filter-" + id());

  public static PlainFilterImpl create() {
    return new PlainFilterImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public Abstraction<PlainPredicate> predicate() {
    return predicate;
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
