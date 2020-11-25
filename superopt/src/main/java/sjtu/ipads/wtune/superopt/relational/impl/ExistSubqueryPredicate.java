package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.SubqueryPredicate;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

public class ExistSubqueryPredicate implements SubqueryPredicate {
  private static final ExistSubqueryPredicate INSTANCE = new ExistSubqueryPredicate();

  private ExistSubqueryPredicate() {}

  public static ExistSubqueryPredicate create() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "Filter*(exists)";
  }

  @Override
  public ColumnSet columns() {
    return null;
  }
}
