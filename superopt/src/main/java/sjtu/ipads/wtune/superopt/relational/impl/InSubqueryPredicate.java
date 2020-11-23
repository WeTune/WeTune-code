package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.SubqueryPredicate;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

public class InSubqueryPredicate implements SubqueryPredicate {
  private final SymbolicColumns column;

  public InSubqueryPredicate(SymbolicColumns column) {
    this.column = column;
  }

  public static InSubqueryPredicate create(SymbolicColumns columns) {
    return new InSubqueryPredicate(columns);
  }

  @Override
  public SymbolicColumns columns() {
    return column;
  }

  @Override
  public String toString() {
    return "Filter*(" + column + " in \u25a1)";
  }
}
