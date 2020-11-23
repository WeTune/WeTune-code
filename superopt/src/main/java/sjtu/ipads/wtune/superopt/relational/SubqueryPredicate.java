package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.relational.impl.ExistSubqueryPredicate;
import sjtu.ipads.wtune.superopt.relational.impl.InSubqueryPredicate;

public interface SubqueryPredicate {
  SymbolicColumns columns();

  static SubqueryPredicate exists() {
    return ExistSubqueryPredicate.create();
  }

  static SubqueryPredicate in(SymbolicColumns columns) {
    return InSubqueryPredicate.create(columns);
  }
}
