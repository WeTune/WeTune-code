package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.TableSym;

public class TableSymImpl implements TableSym {
  private final Object wrapped;
  private int index;

  private TableSymImpl(Object wrapped) {
    this.wrapped = wrapped;
  }

  public static TableSym build(Object wrapped) {
    return new TableSymImpl(wrapped);
  }

  @Override
  public int index() {
    return index;
  }

  @Override
  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> unwrap) {
    return (T) wrapped;
  }

  @Override
  public String toString() {
    return "t" + index;
  }
}
