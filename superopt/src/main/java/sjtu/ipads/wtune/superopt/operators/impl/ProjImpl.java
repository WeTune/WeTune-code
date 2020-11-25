package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Proj;
import sjtu.ipads.wtune.superopt.relational.Projections;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

public class ProjImpl extends BaseOperator implements Proj {
  private Abstraction<Projections> projections;

  private ProjImpl() {}

  public static ProjImpl create() {
    return new ProjImpl();
  }

  @Override
  public Abstraction<Projections> projs() {
    if (projections == null) projections = Abstraction.create(this, "");
    return projections;
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
  protected RelationSchema createOutSchema() {
    return RelationSchema.create(this);
  }

  @Override
  public String toString() {
    return "Proj" + id();
  }
}
