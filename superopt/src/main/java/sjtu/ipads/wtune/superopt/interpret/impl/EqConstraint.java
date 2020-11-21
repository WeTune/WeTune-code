package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.Objects;

public class EqConstraint implements Constraint {
  private final Abstraction<?> left;
  private final Abstraction<?> right;

  private EqConstraint(Abstraction<?> left, Abstraction<?> right) {
    this.left = left;
    this.right = right;
  }

  public static EqConstraint create(Abstraction<?> left, Abstraction<?> right) {
    return new EqConstraint(left, right);
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
  public boolean check(Interpretation context, Abstraction<?> abstraction, Object newInterpret) {
    final boolean isLeft = Objects.equals(left, abstraction);
    final boolean isRight = Objects.equals(right, abstraction);
    if (!isLeft && !isRight) return true; // nothing to do with this constraint

    final Object interpret0 = context.interpret(left);
    final Object interpret1 = context.interpret(right);

    return (isLeft && (interpret1 == null || Objects.equals(interpret1, newInterpret)))
        || (isRight && (interpret0 == null || Objects.equals(interpret0, newInterpret)));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EqConstraint that = (EqConstraint) o;
    return Objects.equals(left, that.left) && Objects.equals(right, that.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right);
  }
}
