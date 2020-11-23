package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.impl.legacy.ColumnSet;
import sjtu.ipads.wtune.superopt.operators.Operator;

public class ColumnSetImpl implements ColumnSet {
  private final Operator operator;

  private ColumnSetImpl(Operator operator) {
    this.operator = operator;
  }

  public static ColumnSetImpl create(Operator operator) {
    return new ColumnSetImpl(operator);
  }

  @Override
  public Operator operator() {
    return operator;
  }
}
