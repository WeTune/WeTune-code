package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

import java.util.Collection;

public class PickSymImpl implements PickSym {
  private final Object wrapped;
  private Collection<TableSym> visibleTables;

  private int index;

  private PickSymImpl(Object wrapped) {
    this.wrapped = wrapped;
  }

  public static PickSym build(Object wrapped) {
    return new PickSymImpl(wrapped);
  }

  @Override
  public Collection<TableSym> visibleTables() {
    return visibleTables;
  }

  @Override
  public void setVisibleTables(Collection<TableSym> visibleTables) {
    this.visibleTables = visibleTables;
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
    return "p" + index;
  }
}
