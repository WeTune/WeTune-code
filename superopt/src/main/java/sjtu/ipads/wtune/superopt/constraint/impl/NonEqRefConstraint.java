package sjtu.ipads.wtune.superopt.constraint.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.Objects;

public class NonEqRefConstraint implements Constraint {
  private final Abstraction<?> left;
  private final Abstraction<?> right;

  private NonEqRefConstraint(Abstraction<?> left, Abstraction<?> right) {
    this.left = left;
    this.right = right;
  }

  public static NonEqRefConstraint create(Abstraction<?> left, Abstraction<?> right) {
    return new NonEqRefConstraint(left, right);
  }

  public Abstraction<?> left() {
    return left;
  }

  public Abstraction<?> right() {
    return right;
  }

  public Abstraction<?> otherSide(Abstraction<?> abs) {
    if (abs == left) return right;
    if (abs == right) return left;
    return null;
  }

  @Override
  public boolean check(Interpretation interpretation, ConstraintSet constraints) {
    final Object leftAssign = interpretation.interpret(left);
    final Object rightAssign = interpretation.interpret(right);
    return leftAssign == null || rightAssign == null || !Objects.equals(leftAssign, rightAssign);
  }

  @Override
  public boolean isConflict(Constraint constraint) {
    return constraint instanceof EqRefConstraint && constraint.isConflict(this);
  }

  @Override
  public boolean isContradiction() {
    return Objects.equals(left, right);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NonEqRefConstraint that = (NonEqRefConstraint) o;
    return (Objects.equals(left, that.left) && Objects.equals(right, that.right))
        || (Objects.equals(left, that.right) && Objects.equals(right, that.left));
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right) + Objects.hash(right, left);
  }

  @Override
  public String toString() {
    return "<" + left + " \u2260 " + right + ">";
  }
}
