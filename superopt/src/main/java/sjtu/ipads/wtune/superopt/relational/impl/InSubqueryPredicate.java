package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.SubqueryPredicate;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

public class InSubqueryPredicate implements SubqueryPredicate {
  private final ColumnSet column;

  public InSubqueryPredicate(ColumnSet column) {
    this.column = column;
  }

  public static InSubqueryPredicate create(ColumnSet columns) {
    return new InSubqueryPredicate(columns);
  }

  @Override
  public ColumnSet columns() {
    return column;
  }

  @Override
  public String toString() {
    return column + " in \u25a1";
  }
}
