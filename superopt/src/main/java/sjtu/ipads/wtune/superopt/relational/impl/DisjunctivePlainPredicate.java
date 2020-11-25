package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.PlainPredicate;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

import java.util.List;

public class DisjunctivePlainPredicate implements PlainPredicate {
  private final ColumnSet columns0;
  private final ColumnSet columns1;

  public DisjunctivePlainPredicate(ColumnSet columns0, ColumnSet columns1) {
    this.columns0 = columns0;
    this.columns1 = columns1;
  }

  public static DisjunctivePlainPredicate create(ColumnSet columns0, ColumnSet columns1) {
    return new DisjunctivePlainPredicate(columns0, columns1);
  }

  @Override
  public List<ColumnSet> columns() {
    return List.of(columns0, columns1);
  }

  @Override
  public String toString() {
    return "P(" + columns0 + ") or P(" + columns1 + ")";
  }
}
