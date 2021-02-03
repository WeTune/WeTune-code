package sjtu.ipads.wtune.symsolver.core.impl;

import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

public class PickSymImpl extends BaseSym implements PickSym {
  private TableSym[] visibleSources;
  private TableSym[][] viableSources;
  private PickSym joined;

  public static PickSym build() {
    return new PickSymImpl();
  }

  @Override
  public TableSym[] visibleSources() {
    return visibleSources;
  }

  @Override
  public TableSym[][] viableSources() {
    return viableSources;
  }

  @Override
  public PickSym joined() {
    return joined;
  }

  @Override
  public void setVisibleSources(TableSym[] visibleSources) {
    this.visibleSources = visibleSources;
  }

  @Override
  public void setViableSources(TableSym[][] viableSources) {
    for (TableSym[] src : viableSources) if (src.length <= 0) throw new IllegalArgumentException();
    this.viableSources = viableSources;
  }

  @Override
  public void setJoined(PickSym joined) {
    this.joined = joined;
  }

  @Override
  public String toString() {
    return "c" + index();
  }
}
