package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

import java.util.Collection;

public interface Reactor {
  void tableEq(Constraint constraint, TableSym tx, TableSym ty);

  void pickEq(Constraint constraint, PickSym px, PickSym py);

  void pickFrom(Constraint constraint, PickSym p, Collection<TableSym> ts);

  void reference(Constraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py);

  void decide(Decision[] decisions);
}
