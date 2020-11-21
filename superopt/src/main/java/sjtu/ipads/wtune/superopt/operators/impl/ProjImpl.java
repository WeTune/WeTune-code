package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.Proj;

public class ProjImpl extends BaseOperator implements Proj {
  public static ProjImpl create() {
    return new ProjImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterProj(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveProj(this);
  }

  @Override
  public String toString() {
    return "Proj";
  }
}
