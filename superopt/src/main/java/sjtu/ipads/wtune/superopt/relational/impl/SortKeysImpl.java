package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.SortKeys;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

import java.util.Objects;

public class SortKeysImpl implements SortKeys {
  private final ColumnSet columns;

  public SortKeysImpl(ColumnSet columns) {
    this.columns = columns;
  }

  public static SortKeysImpl create(ColumnSet from) {
    return new SortKeysImpl(from);
  }

  @Override
  public ColumnSet columns() {
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
