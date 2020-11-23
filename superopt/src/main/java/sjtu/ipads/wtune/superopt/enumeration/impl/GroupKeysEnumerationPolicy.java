package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.relational.GroupKeys;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

public class GroupKeysEnumerationPolicy extends ColumnEnumerationPolicy<GroupKeys> {
  public static GroupKeysEnumerationPolicy create() {
    return new GroupKeysEnumerationPolicy();
  }

  @Override
  protected Iterable<SymbolicColumns> selections(SymbolicColumns source) {
    return source.selections(2);
  }

  @Override
  protected GroupKeys fromSelection(SymbolicColumns columns) {
    return GroupKeys.from(columns);
  }
}
