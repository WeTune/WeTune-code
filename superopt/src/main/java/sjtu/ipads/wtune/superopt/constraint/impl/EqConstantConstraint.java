package sjtu.ipads.wtune.superopt.constraint.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.Objects;

public class EqConstantConstraint implements Constraint {
  private final Abstraction<?> target;
  private final Object constVal;

  private EqConstantConstraint(Abstraction<?> target, Object constVal) {
    this.target = target;
    this.constVal = constVal;
  }

  public static EqConstantConstraint create(Abstraction<?> target, Object constVal) {
    Objects.requireNonNull(target);
    Objects.requireNonNull(constVal);
    return new EqConstantConstraint(target, constVal);
  }

  @Override
  public boolean check(Interpretation interpretation, ConstraintSet constraints) {
    final Object assignment = interpretation.interpret(target);
    return assignment == null || Objects.equals(assignment, constVal);
  }

  @Override
  public String toString() {
    return "<" + target + " = " + constVal + ">";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EqConstantConstraint that = (EqConstantConstraint) o;
    return Objects.equals(target, that.target) && Objects.equals(constVal, that.constVal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(target, constVal);
  }
}
