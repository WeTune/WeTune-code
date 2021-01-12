package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.TableSymImpl;

public interface TableSym extends Sym {
  static TableSym of(Scoped scoped) {
    return TableSymImpl.build(scoped);
  }
}
