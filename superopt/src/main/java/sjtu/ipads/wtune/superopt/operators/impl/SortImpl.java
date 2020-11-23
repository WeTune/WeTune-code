package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Sort;
import sjtu.ipads.wtune.superopt.relational.SortKeys;

public class SortImpl extends BaseOperator implements Sort {
  private final Abstraction<SortKeys> sortKeys = Abstraction.create(this, "sortKeys-" + id());

  public static SortImpl create() {
    return new SortImpl();
  }

  @Override
  protected Operator newInstance() {
    return create();
  }

  @Override
  public Abstraction<SortKeys> sortKeys() {
    return sortKeys;
  }

  @Override
  public void accept0(GraphVisitor visitor) {
    visitor.enterSort(this);
  }

  @Override
  public void leave0(GraphVisitor visitor) {
    visitor.leaveSort(this);
  }

  @Override
  public String toString() {
    return "Sort";
  }
}
