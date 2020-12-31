package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.TableSymImpl;
import sjtu.ipads.wtune.symsolver.smt.Value;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

public interface TableSym extends Sym {
  static TableSym from(Query owner, Object obj) {
    return TableSymImpl.build(owner, obj);
  }
}
