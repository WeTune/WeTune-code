package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.GroupKeys;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.Objects;

public class GroupKeysImpl implements GroupKeys {
  private final SymbolicColumns columns;

  private GroupKeysImpl(SymbolicColumns columns) {
    this.columns = columns;
  }

  public static GroupKeysImpl create(SymbolicColumns columns) {
    return new GroupKeysImpl(columns);
  }

  @Override
  public SymbolicColumns columns() {
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
}
