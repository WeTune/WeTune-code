package sjtu.ipads.wtune.superopt.enumeration.impl;

import sjtu.ipads.wtune.superopt.relational.SortKeys;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

public class SortKeysEnumerationPolicy extends ColumnEnumerationPolicy<SortKeys> {
  public static SortKeysEnumerationPolicy create(){
    return new SortKeysEnumerationPolicy();
  }

  @Override
  protected Iterable<SymbolicColumns> selections(SymbolicColumns source) {
    return source.selections(2);
  }

  @Override
  protected SortKeys fromSelection(SymbolicColumns columns) {
    return SortKeys.from(columns);
  }
}
