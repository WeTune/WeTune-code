package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.Input;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public class InputImpl extends BaseOperator implements Input {
  private InputImpl() {}

  public static InputImpl create() {
    return new InputImpl();
  }

  @Override
  protected Operator newInstance() {
    return this;
  }

  @Override
  public Placeholder table() {
    return fragment().placeholders().getTable(this, 0);
  }

  @Override
  public boolean accept0(OperatorVisitor visitor) {
    return visitor.enterInput(this);
  }

  @Override
  public void leave0(OperatorVisitor visitor) {
    visitor.leaveInput(this);
  }
}
