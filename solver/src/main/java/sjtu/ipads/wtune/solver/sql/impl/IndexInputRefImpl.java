package sjtu.ipads.wtune.solver.sql.impl;

import sjtu.ipads.wtune.solver.sql.expr.IndexInputRef;

public class IndexInputRefImpl implements IndexInputRef {
  private final int index;

  private IndexInputRefImpl(int index) {
    this.index = index;
  }

  public static IndexInputRef create(int index) {
    return new IndexInputRefImpl(index);
  }

  @Override
  public int index() {
    return index;
  }
}
