package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.relational.impl.GroupKeysImpl;

public interface GroupKeys {
  SymbolicColumns columns();

  static GroupKeys from(SymbolicColumns source){
    return GroupKeysImpl.create(source);
  }
}
