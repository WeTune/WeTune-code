package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.InputSource;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

public class InputImpl extends BaseOperator implements Input {
  private final int idx;
  private Abstraction<InputSource> source;

  private InputImpl(int idx) {
    super(0);
    this.idx = idx;
    this.source.setName("t" + idx);
  }

  public static InputImpl create(int idx) {
    return new InputImpl(idx);
  }

  @Override
  protected Operator newInstance() {
    return this;
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterInput(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveInput(this);
  }

  @Override
  public int index() {
    return idx;
  }

  @Override
  protected RelationSchema createOutSchema() {
    return RelationSchema.create(this);
  }

  @Override
  public String interpreterName() {
    return graph().name();
  }

  @Override
  public Abstraction<InputSource> source() {
    if (source == null) source = Abstraction.create(this, "t" + idx);
    return source;
  }

  @Override
  public String toString() {
    return "Input" + id();
  }
}
