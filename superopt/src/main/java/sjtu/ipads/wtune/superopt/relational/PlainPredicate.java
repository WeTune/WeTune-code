package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.relational.impl.DisjunctivePlainPredicate;
import sjtu.ipads.wtune.superopt.relational.impl.SimplePlainPredicate;

import java.util.List;

public interface PlainPredicate {
  List<SymbolicColumns> columns();

  static PlainPredicate simple(SymbolicColumns columns) {
    return SimplePlainPredicate.create(columns);
  }

  static PlainPredicate disjunctive(SymbolicColumns col0, SymbolicColumns col1) {
    return DisjunctivePlainPredicate.create(col0, col1);
  }
}
