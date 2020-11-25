package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.relational.impl.SortKeysImpl;

public interface SortKeys {
  ColumnSet columns();

  static SortKeys from(ColumnSet source) {
    return SortKeysImpl.create(source);
  }
}
