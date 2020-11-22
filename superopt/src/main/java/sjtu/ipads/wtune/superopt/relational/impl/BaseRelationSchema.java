package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.*;
import sjtu.ipads.wtune.superopt.relational.ColumnSet;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

import java.util.Objects;

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
  public ColumnSet columns(Interpretation interpretation) {
    return operator.prev()[0].outSchema().columns(interpretation);
  }

  @Override
  public RelationSchema nonTrivialSource() {
    if (operator instanceof Join
        || operator instanceof Input
        || operator instanceof Proj
        || operator instanceof Agg) return this;
    return operator.prev()[0].outSchema().nonTrivialSource();
  }

  @Override
  public boolean schemaEquals(RelationSchema other, Interpretation interpretation) {
    return Objects.equals(columns(interpretation), other.columns(interpretation));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
