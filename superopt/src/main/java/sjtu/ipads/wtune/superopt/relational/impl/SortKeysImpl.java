package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.SortKeys;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.Objects;

public class SortKeysImpl implements SortKeys {
  private final SymbolicColumns columns;

  public SortKeysImpl(SymbolicColumns columns) {
    this.columns = columns;
  }

  public static SortKeysImpl create(SymbolicColumns from) {
    return new SortKeysImpl(from);
  }

  @Override
  public SymbolicColumns columns() {
    return columns;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SortKeysImpl sortKeys = (SortKeysImpl) o;
    return Objects.equals(columns, sortKeys.columns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columns);
  }

  @Override
  public String toString() {
    return "SortKeys(" + columns + ')';
  }
}
