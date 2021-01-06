package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.TableSymImpl;

public interface TableSym extends Sym {
  static TableSym from(Query owner, Object obj) {
    return TableSymImpl.build(owner, obj);
  }
}
