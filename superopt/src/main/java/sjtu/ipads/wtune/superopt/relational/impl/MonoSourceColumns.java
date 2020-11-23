package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

import java.util.Collections;
import java.util.Set;

public abstract class MonoSourceColumns implements SymbolicColumns {
  @Override
  public SymbolicColumns concat(SymbolicColumns other) {
    return MultiSourceSymbolicColumns.empty().concat0(this).concat0(other);
  }

  @Override
  public Set<SymbolicColumns> selections(int max) {
    return Collections.singleton(copy());
  }

  public abstract MonoSourceColumns copy();
}
