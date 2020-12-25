package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.TableSymImpl;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

public interface TableSym extends Indexed {
  <T> T unwrap(Class<T> cls);

  static TableSym from(Object obj) {
    return TableSymImpl.build(obj);
  }

  static TableSym from(Object obj, int i) {
    final TableSym t = from(obj);
    t.setIndex(i);
    return t;
  }
}
