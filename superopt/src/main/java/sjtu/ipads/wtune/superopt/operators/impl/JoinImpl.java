package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.operators.Join;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

public class JoinImpl extends BaseOperator implements Join {
  private JoinImpl() {
    super(2);
  }

  public static JoinImpl create() {
    return new JoinImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterJoin(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveJoin(this);
  }

  @Override
  protected RelationSchema createOutSchema() {
    return RelationSchema.create(this);
  }

  @Override
  public String toString() {
    return "Join" + id();
  }
}
