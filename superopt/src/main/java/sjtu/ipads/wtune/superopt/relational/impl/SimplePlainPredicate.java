package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.PlainPredicate;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

import java.util.Collections;
import java.util.List;

public class SimplePlainPredicate implements PlainPredicate {
  private final ColumnSet columns;

  public SimplePlainPredicate(ColumnSet columns) {
    this.columns = columns;
  }

  public static SimplePlainPredicate create(ColumnSet columns) {
    return new SimplePlainPredicate(columns);
  }

  @Override
  public List<ColumnSet> columns() {
    return Collections.singletonList(columns);
  }

  @Override
  public String toString() {
    return "P(" + columns + ")";
  }
}
