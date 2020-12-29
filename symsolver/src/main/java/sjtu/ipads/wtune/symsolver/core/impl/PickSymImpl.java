package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

import java.util.Collection;
import java.util.List;

public class PickSymImpl implements PickSym {
  private final Object wrapped;
  private List<TableSym> visibleSources;
  private Collection<? extends Collection<TableSym>> viableSources;
  private PickSym joined;

  private int index;

  private PickSymImpl(Object wrapped) {
    this.wrapped = wrapped;
  }

  public static PickSym build(Object wrapped) {
    return new PickSymImpl(wrapped);
  }

  @Override
  public List<TableSym> visibleSources() {
    return visibleSources;
  }

  @Override
  public Collection<? extends Collection<TableSym>> viableSources() {
    return viableSources;
  }

  @Override
  public PickSym joined() {
    return joined;
  }

  @Override
  public void setVisibleSources(List<TableSym> visibleSources) {
    this.visibleSources = visibleSources;
  }

  @Override
  public void setViableSources(Collection<? extends Collection<TableSym>> viableSources) {
    this.viableSources = viableSources;
  }

  @Override
  public void setJoined(PickSym joined) {
    this.joined = joined;
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
