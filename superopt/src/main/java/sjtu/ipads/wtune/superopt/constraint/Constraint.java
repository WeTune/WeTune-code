package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.constraint.impl.*;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

import java.util.List;

import static java.util.Objects.requireNonNull;

public interface Constraint {
  boolean check(Interpretation context, ConstraintSet constraintSet);

  default boolean check(Interpretation context) {
    return check(context, null);
  }

  default boolean isConflict(Constraint constraint) {
    return false;
  }

  default Constraint buildTransitive(Constraint constraint) {
    return null;
  }

  default boolean isTautology() {
    return false;
  }

  default boolean isContradiction() {
    return false;
  }

  static Constraint refEq(Abstraction<?> left, Abstraction<?> right) {
    return EqRefConstraint.create(requireNonNull(left), requireNonNull(right));
  }

  static Constraint refNonEq(Abstraction<?> left, Abstraction<?> right) {
    return NonEqRefConstraint.create(requireNonNull(left), requireNonNull(right));
  }

  static Constraint constEq(Abstraction<?> abs, Object obj) {
    return EqConstantConstraint.create(requireNonNull(abs), requireNonNull(obj));
  }

  static List<Constraint> schemaEq(RelationSchema s0, RelationSchema s1) {
    return EqSchemaConstraint.create(requireNonNull(s0), requireNonNull(s1));
  }

  static Constraint schemaShapeEq(RelationSchema s0, RelationSchema s1) {
    return EqSchemaShapeConstraint.create(requireNonNull(s0), requireNonNull(s1));
  }

  static Constraint contradiction() {
    return Contradiction.create();
  }

  static Tautology tautology() {
    return Tautology.create();
  }
}
