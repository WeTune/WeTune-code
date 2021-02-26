package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;
import sjtu.ipads.wtune.superopt.fragment.Proj;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public class ProjImpl extends BaseOperator implements Proj {
  private ProjImpl() {}

  public static ProjImpl create() {
    return new ProjImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public Placeholder fields() {
    return fragment().placeholders().getPick(this, 0);
  }

  @Override
  public boolean accept0(OperatorVisitor visitor) {
    return visitor.enterProj(this);
  }

  @Override
  public void leave0(OperatorVisitor visitor) {
    visitor.leaveProj(this);
  }
}
