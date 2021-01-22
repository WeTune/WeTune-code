package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.operator.SubqueryFilter;

public class SubqueryFilterImpl extends BaseOperator implements SubqueryFilter {
  private final Placeholder fields;

  private SubqueryFilterImpl() {
    fields = newPlaceholder("c");
  }

  public static SubqueryFilterImpl create() {
    return new SubqueryFilterImpl();
  }

  @Override
  public void setPlaceholders(String[] str) {
    fields.setIndex(Integer.parseInt(str[1].substring(str[1].indexOf('c') + 1)));
  }

  @Override
  public Placeholder fields() {
    return fields;
  }

  @Override
  public boolean accept0(GraphVisitor visitor) {
    return visitor.enterSubqueryFilter(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveSubqueryFilter(this);
  }

  @Override
  protected Operator newInstance() {
    return create();
  }
}
