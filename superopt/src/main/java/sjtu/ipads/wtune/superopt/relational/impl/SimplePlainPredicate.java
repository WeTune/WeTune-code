package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.PlainPredicate;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.Collections;
import java.util.List;

public class SimplePlainPredicate implements PlainPredicate {
  private final SymbolicColumns columns;

  public SimplePlainPredicate(SymbolicColumns columns) {
    this.columns = columns;
  }

  public static SimplePlainPredicate create(SymbolicColumns columns) {
    return new SimplePlainPredicate(columns);
  }

  @Override
  public List<SymbolicColumns> columns() {
    return Collections.singletonList(columns);
  }

  @Override
  public String toString() {
    return "Filter(P(" + columns + "))";
  }
}
