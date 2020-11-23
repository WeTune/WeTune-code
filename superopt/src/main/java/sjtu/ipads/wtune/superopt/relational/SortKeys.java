package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.relational.impl.SortKeysImpl;

public interface SortKeys {
  SymbolicColumns columns();

  static SortKeys from(SymbolicColumns source) {
    return SortKeysImpl.create(source);
  }
}
