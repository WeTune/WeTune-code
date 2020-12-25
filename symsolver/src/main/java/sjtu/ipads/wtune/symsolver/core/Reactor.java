package sjtu.ipads.wtune.symsolver.core;

import java.util.Collection;

public interface Reactor {
  void tableEq(Constraint constraint, TableSym tx, TableSym ty);

  void pickEq(Constraint constraint, PickSym px, PickSym py);

  void pickFrom(Constraint constraint, PickSym p, Collection<TableSym> ts);

  void reference(Constraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py);

  default void decide(Decision[] decisions) {
    for (Decision decision : decisions) decision.decide(this);
  }
}
