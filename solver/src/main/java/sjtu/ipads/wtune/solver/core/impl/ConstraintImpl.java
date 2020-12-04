package sjtu.ipads.wtune.solver.core.impl;

import sjtu.ipads.wtune.solver.core.Constraint;

public class ConstraintImpl implements Constraint {
  private final Object expr;
  private final String desc;

  public ConstraintImpl(Object expr, String desc) {
    this.expr = expr;
    this.desc = desc == null ? "" : desc;
  }

  public static ConstraintImpl create(Object z3Expr, String desc) {
    return new ConstraintImpl(z3Expr, desc);
  }

  @Override
  public <T> T unwrap(Class<T> clazz) {
    return (T) expr;
  }

  @Override
  public String name() {
    return desc;
  }

  @Override
  public String toString() {
    return desc;
  }
}
