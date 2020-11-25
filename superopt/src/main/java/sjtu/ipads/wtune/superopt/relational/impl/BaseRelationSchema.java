package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.*;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;

public class BaseRelationSchema<T extends Operator> implements RelationSchema {
  protected final T operator;

  protected BaseRelationSchema(T operator) {
    this.operator = operator;
  }

  public static <T extends Operator> BaseRelationSchema<T> create(T op) {
    return new BaseRelationSchema<T>(op);
  }

  @Override
  public Operator op() {
    return operator;
  }

  @Override
  public ColumnSet symbolicColumns(Interpretation interpretation) {
    return operator.prev()[0].outSchema().symbolicColumns(interpretation);
  }

  @Override
  public RelationSchema nonTrivialSource() {
    if (operator instanceof Join
        || operator instanceof Input
        || operator instanceof Proj
        || operator instanceof Agg
        || operator instanceof Union) return this;
    return operator.prev()[0].outSchema().nonTrivialSource();
  }

  @Override
  public List<List<Constraint>> enforceEq(RelationSchema other, Interpretation interpretation) {
    final ColumnSet thisColumns = symbolicColumns(interpretation);
    final ColumnSet otherColumns = other.symbolicColumns(interpretation);

    return thisColumns == null || otherColumns == null
        ? singletonList(singletonList(Constraint.tautology()))
        : thisColumns.enforceEq(otherColumns, interpretation);
  }

  @Override
  public boolean shapeEquals(RelationSchema other, Interpretation interpretation) {
    return true; // TODO
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
