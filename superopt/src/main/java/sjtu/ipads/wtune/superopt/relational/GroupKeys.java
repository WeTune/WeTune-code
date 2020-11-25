package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.relational.impl.GroupKeysImpl;

public interface GroupKeys {
  ColumnSet columns();

  static GroupKeys from(ColumnSet source){
    return GroupKeysImpl.create(source);
  }
}
