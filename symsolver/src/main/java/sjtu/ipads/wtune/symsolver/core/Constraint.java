package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.PickEqImpl;
import sjtu.ipads.wtune.symsolver.core.impl.PickFromImpl;
import sjtu.ipads.wtune.symsolver.core.impl.ReferenceImpl;
import sjtu.ipads.wtune.symsolver.core.impl.TableEqImpl;
import sjtu.ipads.wtune.symsolver.search.Decision;

import java.util.Collection;

public interface Constraint extends Decision {
  static Constraint tableEq(TableSym tx, TableSym ty) {
    return TableEqImpl.build(tx, ty);
  }

  static Constraint pickEq(PickSym px, PickSym py) {
    return PickEqImpl.build(px, py);
  }

  static Constraint pickFrom(PickSym p, Collection<TableSym> ts) {
    return PickFromImpl.build(p, ts);
  }

  static Constraint reference(TableSym tx, PickSym px, TableSym ty, PickSym py) {
    return ReferenceImpl.build(tx, px, ty, py);
  }

  Kind kind();

  Sym[] targets();

  enum Kind {
    TableEq,
    PickEq,
    PickFrom,
    Reference
  }
}
