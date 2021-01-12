package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.operator.PlainFilter;

public class PlainFilterImpl extends BaseOperator implements PlainFilter {
  private final Placeholder fields;
  private final Placeholder predicate;

  private PlainFilterImpl() {
    fields = newPlaceholder("c");
    predicate = newPlaceholder("p");
  }

  public static PlainFilterImpl create() {
    return new PlainFilterImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public Placeholder fields() {
    return fields;
  }

  @Override
  public Placeholder predicate() {
    return predicate;
  }

  @Override
  public boolean accept0(GraphVisitor visitor) {
    return visitor.enterPlainFilter(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leavePlainFilter(this);
  }
}
