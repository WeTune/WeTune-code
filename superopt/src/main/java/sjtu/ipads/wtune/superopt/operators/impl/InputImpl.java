package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.Relation;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.Input;

import java.util.Objects;

public class InputImpl extends BaseOperator implements Input {
  private final int idx;
  private final Abstraction<Relation> relation;

  private InputImpl(int idx) {
    super(0);
    this.idx = idx;
    this.relation = Abstraction.create("t" + idx);
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
  public Abstraction<Relation> relation() {
    return relation;
  }

  @Override
  public String toString() {
    return "i" + idx;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    return o instanceof Input;
  }

  @Override
  public int hashCode() {
    return Input.class.hashCode();
  }
}
