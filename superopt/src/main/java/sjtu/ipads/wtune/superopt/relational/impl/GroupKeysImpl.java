package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.GroupKeys;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;

import java.util.Objects;

public class GroupKeysImpl implements GroupKeys {
  private final ColumnSet columns;

  private GroupKeysImpl(ColumnSet columns) {
    this.columns = columns;
  }

  public static GroupKeysImpl create(ColumnSet columns) {
    return new GroupKeysImpl(columns);
  }

  @Override
  public ColumnSet columns() {
    return columns;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GroupKeysImpl that = (GroupKeysImpl) o;
    return Objects.equals(columns, that.columns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columns);
  }

  @Override
  public String toString() {
    return "GroupKey(" + columns + ")";
  }
}
