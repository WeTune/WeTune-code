package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.PickSymImpl;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

import java.util.Collection;

public interface PickSym extends Indexed {
  Collection<TableSym> visibleTables();

  void setVisibleTables(Collection<TableSym> visibleTables);

  <T> T unwrap(Class<T> unwrap);

  static PickSym from(Object obj) {
    return PickSymImpl.build(obj);
  }

  static PickSym from(Object obj, int i) {
    final PickSym p = from(obj);
    p.setIndex(i);
    return p;
  }
}
