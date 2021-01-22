package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.Input;
import sjtu.ipads.wtune.superopt.operator.Operator;

public class InputImpl extends BaseOperator implements Input {
  private final Placeholder table;

  private InputImpl() {
    this.table = newPlaceholder("t");
  }

  public static InputImpl create() {
    return new InputImpl();
  }

  @Override
  public void setPlaceholders(String[] str) {
    table.setIndex(Integer.parseInt(str[1].substring(str[1].indexOf('t') + 1)));
  }

  @Override
  protected Operator newInstance() {
    return this;
  }

  @Override
  public Placeholder table() {
    return table;
  }

  @Override
  public boolean accept0(GraphVisitor visitor) {
    return visitor.enterInput(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveInput(this);
  }
}
