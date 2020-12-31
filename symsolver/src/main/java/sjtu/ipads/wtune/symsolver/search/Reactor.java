package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

public interface Reactor {
  void tableEq(Constraint constraint, TableSym tx, TableSym ty);

  void pickEq(Constraint constraint, PickSym px, PickSym py);

  void pickFrom(Constraint constraint, PickSym p, TableSym... src);

  void reference(Constraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py);

  void decide(Decision[] decisions);
}
