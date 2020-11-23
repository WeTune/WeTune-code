package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.PlainPredicate;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.List;

public class DisjunctivePlainPredicate implements PlainPredicate {
  private final SymbolicColumns columns0;
  private final SymbolicColumns columns1;

  public DisjunctivePlainPredicate(SymbolicColumns columns0, SymbolicColumns columns1) {
    this.columns0 = columns0;
    this.columns1 = columns1;
  }

  public static DisjunctivePlainPredicate create(
      SymbolicColumns columns0, SymbolicColumns columns1) {
    return new DisjunctivePlainPredicate(columns0, columns1);
  }

  @Override
  public List<SymbolicColumns> columns() {
    return List.of(columns0, columns1);
  }

  @Override
  public String toString() {
    return "Filter(P(" + columns0 + ") or P(" + columns1 + "))";
  }
}
