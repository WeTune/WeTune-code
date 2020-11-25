package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.relational.impl.ExistSubqueryPredicate;
import sjtu.ipads.wtune.superopt.relational.impl.InSubqueryPredicate;

public interface SubqueryPredicate {
  ColumnSet columns();

  static SubqueryPredicate exists() {
    return ExistSubqueryPredicate.create();
  }

  static SubqueryPredicate in(ColumnSet columns) {
    return InSubqueryPredicate.create(columns);
  }
}
