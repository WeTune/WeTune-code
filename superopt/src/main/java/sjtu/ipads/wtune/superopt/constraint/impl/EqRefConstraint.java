package sjtu.ipads.wtune.superopt.constraint.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.Objects;

public class EqRefConstraint implements Constraint {
  private final Abstraction<?> left;
  private final Abstraction<?> right;

  private EqRefConstraint(Abstraction<?> left, Abstraction<?> right) {
    this.left = left;
    this.right = right;
  }

  public static EqRefConstraint create(Abstraction<?> left, Abstraction<?> right) {
    return new EqRefConstraint(left, right);
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

  public boolean check(Interpretation interpretation, ConstraintSet constraints) {
    final Object leftAssign = interpretation.interpret(left);
    final Object rightAssign = interpretation.interpret(right);
    return leftAssign == null || rightAssign == null || Objects.equals(leftAssign, rightAssign);
  }

  @Override
  public boolean isConflict(Constraint constraint) {
    if (!(constraint instanceof NonEqRefConstraint)) return false;
    final Abstraction<?> otherLeft = ((NonEqRefConstraint) constraint).left();
    final Abstraction<?> otherRight = ((NonEqRefConstraint) constraint).right();
    return (Objects.equals(left, otherLeft) && Objects.equals(right, otherRight))
        || (Objects.equals(left, otherRight) && Objects.equals(right, otherLeft));
  }

  @Override
  public boolean isTautology() {
    return Objects.equals(left, right);
  }

  @Override
  public Constraint buildTransitive(Constraint other) {
    if (!(other instanceof EqRefConstraint)) return null;
    final EqRefConstraint otherEq = (EqRefConstraint) other;
    if (Objects.equals(left, otherEq.left)) {
      if (!Objects.equals(right, otherEq.right)) return create(right, otherEq.right);
      else return null;
    }
    if (Objects.equals(left, otherEq.right)) {
      if (!Objects.equals(right, otherEq.left)) return create(right, otherEq.left);
      else return null;

    } else if (Objects.equals(right, otherEq.left)) {
      if (!Objects.equals(left, otherEq.right)) return create(left, otherEq.right);
      else return null;

    } else if (Objects.equals(right, otherEq.right)) {
      if (!Objects.equals(left, otherEq.left)) return create(left, otherEq.left);
      else return null;

    } else return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EqRefConstraint that = (EqRefConstraint) o;
    return (Objects.equals(left, that.left) && Objects.equals(right, that.right))
        || (Objects.equals(left, that.right) && Objects.equals(right, that.left));
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right) + Objects.hash(right, left);
  }

  @Override
  public String toString() {
    return "<" + left + " = " + right + ">";
  }
}
