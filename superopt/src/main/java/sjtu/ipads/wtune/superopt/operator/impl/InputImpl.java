package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.Input;
import sjtu.ipads.wtune.superopt.operator.Operator;

public class InputImpl extends BaseOperator implements Input {
  private final Placeholder table;
  private int idx;

  private InputImpl() {
    this.table = newPlaceholder("t");
  }

  public static InputImpl create() {
    return new InputImpl();
  }

  @Override
  protected Operator newInstance() {
    return this;
  }

  @Override
  public int index() {
    return idx;
  }

  @Override
  public void setIndex(int index) {
    this.idx = index;
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
