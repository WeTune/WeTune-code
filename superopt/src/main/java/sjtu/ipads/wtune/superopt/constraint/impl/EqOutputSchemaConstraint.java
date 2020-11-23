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
import sjtu.ipads.wtune.superopt.relational.impl.JoinSchema;
import sjtu.ipads.wtune.superopt.relational.impl.ProjSchema;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static sjtu.ipads.wtune.superopt.constraint.Constraint.constEq;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.refEq;
import static sjtu.ipads.wtune.superopt.relational.Projections.selectAll;

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

  public static Set<Constraint> fineGrainedSchemaEqConstraint(
      RelationSchema left, RelationSchema right) {
    final Set<Constraint> ret = new HashSet<>();

    left = left.nonTrivialSource();
    right = right.nonTrivialSource();

    if (left instanceof ProjSchema && right instanceof ProjSchema) {
      ret.add(refEq(((Proj) left.op()).projs(), ((Proj) right.op()).projs()));

    } else if (left instanceof InputSchema && right instanceof InputSchema) {
      ret.add(refEq(((Input) left.op()).relation(), ((Input) right.op()).relation()));

    } else if (left instanceof ProjSchema && right instanceof InputSchema) {
      ret.add(constEq(((Proj) left.op()).projs(), selectAll(((Input) right.op()).relation())));

    } else if (left instanceof InputSchema && right instanceof ProjSchema) {
      ret.add(constEq(((Proj) right.op()).projs(), selectAll(((Input) left.op()).relation())));

    } else if (left instanceof AggSchema && right instanceof AggSchema) {
      ret.add(refEq(((Agg) left.op()).groupKeys(), ((Agg) right.op()).groupKeys()));
      ret.add(refEq(((Agg) left.op()).aggFuncs(), ((Agg) right.op()).aggFuncs()));

    } else if (left instanceof AggSchema && right instanceof InputSchema) {
      return null;

    } else if (left instanceof InputSchema && right instanceof AggSchema) {
      return null;

    } else if (left instanceof JoinSchema && right instanceof InputSchema) {
      return null;

    } else if (left instanceof InputSchema && right instanceof JoinSchema) {
      return null;
    }
    return ret;
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
