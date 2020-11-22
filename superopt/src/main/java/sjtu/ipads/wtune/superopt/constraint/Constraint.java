package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.constraint.impl.EqConstantConstraint;
import sjtu.ipads.wtune.superopt.constraint.impl.EqOutputSchemaConstraint;
import sjtu.ipads.wtune.superopt.constraint.impl.EqRefConstraint;
import sjtu.ipads.wtune.superopt.constraint.impl.NonConflictConstraint;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

public interface Constraint {
  boolean check(Interpretation context, Abstraction<?> abstraction, Object interpretation);

  default boolean recheck(Interpretation context) {
    return true;
  }

  static Constraint refEq(Abstraction<?> left, Abstraction<?> right) {
    return EqRefConstraint.create(left, right);
  }

  static Constraint constEq(Abstraction<?> abs, Object obj) {
    return EqConstantConstraint.create(abs, obj);
  }

  static Constraint nonConflict() {
    return NonConflictConstraint.INSTANCE;
  }

  static Constraint schemaEq(RelationSchema s0, RelationSchema s1) {
    return EqOutputSchemaConstraint.create(s0, s1);
  }
}
