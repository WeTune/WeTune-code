package sjtu.ipads.wtune.superopt.constraint.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Agg;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.operators.Proj;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;
import sjtu.ipads.wtune.superopt.relational.impl.AggSchema;
import sjtu.ipads.wtune.superopt.relational.impl.InputSchema;
import sjtu.ipads.wtune.superopt.relational.impl.ProjSchema;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.constEq;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.refEq;
import static sjtu.ipads.wtune.superopt.relational.Projections.selectAll;

// two schemas contains exactly same set of columns
public class EqSchemaConstraint implements Constraint {
  private final RelationSchema left;
  private final RelationSchema right;

  private EqSchemaConstraint(RelationSchema left, RelationSchema right) {
    this.left = left;
    this.right = right;
  }

  public static List<Constraint> create(RelationSchema left, RelationSchema right) {
    left = left.nonTrivialSource();
    right = right.nonTrivialSource();

    if (left instanceof ProjSchema && right instanceof ProjSchema) {
      return singletonList(refEq(((Proj) left.op()).projs(), ((Proj) right.op()).projs()));

    } else if (left instanceof InputSchema && right instanceof InputSchema) {
      return singletonList(refEq(((Input) left.op()).source(), ((Input) right.op()).source()));

    } else if (left instanceof ProjSchema && right instanceof InputSchema) {
      return singletonList(
          constEq(
              ((Proj) left.op()).projs(), selectAll(right.op(), ((Input) right.op()).source())));

    } else if (left instanceof InputSchema && right instanceof ProjSchema) {
      return singletonList(
          constEq(
              ((Proj) right.op()).projs(), selectAll(right.op(), ((Input) left.op()).source())));

    } else if (left instanceof AggSchema && right instanceof AggSchema) {
      return List.of(
          refEq(((Agg) left.op()).groupKeys(), ((Agg) right.op()).groupKeys()),
          refEq(((Agg) left.op()).aggFuncs(), ((Agg) right.op()).aggFuncs()));

    } else {
      return singletonList(new EqSchemaConstraint(left, right));
    }
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
    EqSchemaConstraint that = (EqSchemaConstraint) o;
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
