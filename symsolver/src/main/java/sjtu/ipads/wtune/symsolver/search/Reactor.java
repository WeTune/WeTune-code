package sjtu.ipads.wtune.symsolver.search;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

public interface Reactor {
  void tableEq(DecidableConstraint constraint, TableSym tx, TableSym ty);

  void pickEq(DecidableConstraint constraint, PickSym px, PickSym py);

  void predicateEq(DecidableConstraint constraint, PredicateSym px, PredicateSym py);

  void pickFrom(DecidableConstraint constraint, PickSym p, TableSym... src);

  void reference(DecidableConstraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py);

  void decide(Decision... decisions);
}
