package sjtu.ipads.wtune.symsolver.core;

import com.google.common.collect.Iterables;
import sjtu.ipads.wtune.symsolver.core.impl.PickSymImpl;

import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;

public interface PickSym extends Sym {
  static PickSym of(Scoped scoped) {
    return PickSymImpl.build(scoped);
  }

  TableSym[] visibleSources();

  TableSym[][] viableSources();

  PickSym joined();

  void setVisibleSources(TableSym[] visibleSources);

  void setViableSources(TableSym[][] viableSources);

  void setJoined(PickSym joined);

  default void setViableSources(Iterable<? extends Iterable<TableSym>> viableSources) {
    setViableSources(
        stream(viableSources)
            .map(it -> Iterables.toArray(it, TableSym.class))
            .filter(it -> it.length > 0)
            .toArray(TableSym[][]::new));
  }
}
