package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.PickSymImpl;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

import java.util.Collection;
import java.util.List;

public interface PickSym extends Indexed, Sym {
  static PickSym from(Object obj) {
    return PickSymImpl.build(obj);
  }

  static PickSym from(Object obj, int i) {
    final PickSym p = from(obj);
    p.setIndex(i);
    return p;
  }

  List<TableSym> visibleSources();

  Collection<? extends Collection<TableSym>> viableSources();

  PickSym joined();

  void setVisibleSources(List<TableSym> visibleSources);

  void setViableSources(Collection<? extends Collection<TableSym>> viableSources);

  void setJoined(PickSym joined);
}
