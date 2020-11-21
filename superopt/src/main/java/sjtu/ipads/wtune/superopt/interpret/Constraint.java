package sjtu.ipads.wtune.superopt.interpret;

import sjtu.ipads.wtune.superopt.interpret.impl.EqConstraint;
import sjtu.ipads.wtune.superopt.interpret.impl.NonConflictConstraint;

public interface Constraint {
  boolean check(Interpretation context, Abstraction<?> abstraction, Object interpretation);

  static Constraint eq(Abstraction<?> left, Abstraction<?> right) {
    return EqConstraint.create(left, right);
  }

  static Constraint nonConflict() {
    return NonConflictConstraint.INSTANCE;
  }
}
