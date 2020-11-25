package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.relational.impl.DisjunctivePlainPredicate;
import sjtu.ipads.wtune.superopt.relational.impl.SimplePlainPredicate;

import java.util.List;

public interface PlainPredicate {
  List<ColumnSet> columns();

  static PlainPredicate simple(ColumnSet columns) {
    return SimplePlainPredicate.create(columns);
  }

  static PlainPredicate disjunctive(ColumnSet col0, ColumnSet col1) {
    return DisjunctivePlainPredicate.create(col0, col1);
  }
}
