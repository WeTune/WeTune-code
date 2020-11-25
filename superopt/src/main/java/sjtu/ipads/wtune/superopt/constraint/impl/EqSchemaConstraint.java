package sjtu.ipads.wtune.superopt.constraint.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;

// two schemas contains exactly same set of columns
public class EqSchemaConstraint implements Constraint {
  private final RelationSchema left;
  private final RelationSchema right;

  private EqSchemaConstraint(RelationSchema left, RelationSchema right) {
    this.left = left;
    this.right = right;
  }

  public static List<Constraint> create(RelationSchema left, RelationSchema right) {
    final RelationSchema leftSource = left.nonTrivialSource();
    final RelationSchema rightSource = right.nonTrivialSource();
    return singletonList(new EqSchemaConstraint(leftSource, rightSource));
  }
  //  public static List<Constraint> create(RelationSchema left, RelationSchema right) {
  //    left = left.nonTrivialSource();
  //    right = right.nonTrivialSource();
  //
  //    if (left instanceof ProjSchema && right instanceof ProjSchema) {
  //      return singletonList(refEq(((Proj) left.op()).projs(), ((Proj) right.op()).projs()));
  //
  //    } else if (left instanceof InputSchema && right instanceof InputSchema) {
  //      return singletonList(refEq(((Input) left.op()).source(), ((Input) right.op()).source()));
  //
  //    } else if (left instanceof ProjSchema && right instanceof InputSchema) {
  //      return singletonList(
  //          constEq(
  //              ((Proj) left.op()).projs(), selectAll(right.op(), ((Input)
  // right.op()).source())));
  //
  //    } else if (left instanceof InputSchema && right instanceof ProjSchema) {
  //      return singletonList(
  //          constEq(
  //              ((Proj) right.op()).projs(), selectAll(right.op(), ((Input)
  // left.op()).source())));
  //
  //    } else if (left instanceof AggSchema && right instanceof AggSchema) {
  //      return List.of(
  //          refEq(((Agg) left.op()).groupKeys(), ((Agg) right.op()).groupKeys()),
  //          refEq(((Agg) left.op()).aggFuncs(), ((Agg) right.op()).aggFuncs()));
  //
  //    } else {
  //      return singletonList(new EqSchemaConstraint(left, right));
  //    }
  //  }

  @Override
  public boolean check(Interpretation context, ConstraintSet constraints) {
    final List<List<Constraint>> conditions = left.enforceEq(right, context);

    outer:
    for (List<Constraint> condition : conditions) {
      for (Constraint constraint : condition) {
        if (!constraints.checkNonConflict(constraint)) continue outer;
        if (!constraint.check(context)) continue outer;
      }
      return true;
    }

    return false;
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
