package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.Proj;

public class ProjImpl extends BaseOperator implements Proj {
  private final Placeholder fields;

  private ProjImpl() {
    fields = newPlaceholder("c");
  }

  public static ProjImpl create() {
    return new ProjImpl();
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
  public boolean accept0(GraphVisitor visitor) {
    return visitor.enterProj(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveProj(this);
  }
}
