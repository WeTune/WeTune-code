package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.constraint.impl.*;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

import java.util.List;

public interface Constraint {
  boolean check(Interpretation context, Abstraction<?> abstraction, Object interpretation);

  default boolean recheck(Interpretation context) {
    return true;
  }

  default boolean isConflict(Constraint constraint) {
    return false;
  }

  default Constraint transitive(Constraint constraint) {
    return null;
  }

  static Constraint refEq(Abstraction<?> left, Abstraction<?> right) {
    return EqRefConstraint.create(left, right);
  }

  static Constraint refNonEq(Abstraction<?> left, Abstraction<?> right) {
    return NonEqRefConstraint.create(left, right);
  }

  static Constraint constEq(Abstraction<?> abs, Object obj) {
    return EqConstantConstraint.create(abs, obj);
  }

  static Constraint nonConflict() {
    return NonConflictConstraint.INSTANCE;
  }

  static List<Constraint> schemaEq(RelationSchema s0, RelationSchema s1) {
    return EqSchemaConstraint.create(s0, s1);
  }

  static Constraint schemaShapeEq(RelationSchema s0, RelationSchema s1) {
    return EqSchemaShapeConstraint.create(s0, s1);
  }
}
