package sjtu.ipads.wtune.symsolver;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.core.impl.*;
import sjtu.ipads.wtune.symsolver.search.Decision;

public interface DecidableConstraint extends Constraint, Decision, Comparable<DecidableConstraint> {
  static DecidableConstraint tableEq(TableSym tx, TableSym ty) {
    return DecidableTableEq.build(tx, ty);
  }

  static DecidableConstraint pickEq(PickSym px, PickSym py) {
    return DecidablePickEq.build(px, py);
  }

  static DecidableConstraint predicateEq(PredicateSym px, PredicateSym py) {
    return DecidablePredicateEq.build(px, py);
  }

  static DecidableConstraint pickFrom(PickSym p, TableSym... ts) {
    return DecidablePickFrom.build(p, ts);
  }

  static DecidableConstraint reference(TableSym tx, PickSym px, TableSym ty, PickSym py) {
    return DecidableReference.build(tx, px, ty, py);
  }
}
