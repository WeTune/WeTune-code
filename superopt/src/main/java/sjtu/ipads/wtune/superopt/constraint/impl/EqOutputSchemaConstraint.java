package sjtu.ipads.wtune.superopt.constraint.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

import java.util.Objects;

public class EqOutputSchemaConstraint implements Constraint {
  private final RelationSchema left;
  private final RelationSchema right;

  private EqOutputSchemaConstraint(RelationSchema left, RelationSchema right) {
    this.left = left;
    this.right = right;
  }

  public static EqOutputSchemaConstraint create(RelationSchema left, RelationSchema right) {
    return new EqOutputSchemaConstraint(left, right);
  }

  @Override
  public boolean check(Interpretation context, Abstraction<?> abstraction, Object interpretation) {
    return true;
  }

  @Override
  public boolean recheck(Interpretation context) {
    return left.columns(context) == null
        || right.columns(context) == null
        || left.schemaEquals(right, context);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EqOutputSchemaConstraint that = (EqOutputSchemaConstraint) o;
    return Objects.equals(left, that.left) && Objects.equals(right, that.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right);
  }

  @Override
  public String toString() {
    return "<" + left + " = " + right + ">";
  }
}
