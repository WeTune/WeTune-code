package sjtu.ipads.wtune.solver.sql.impl;

import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.sql.ColumnRef;
import sjtu.ipads.wtune.solver.sql.ExprColumnRef;
import sjtu.ipads.wtune.solver.sql.expr.Expr;

public class ExprColumnRefImpl extends BaseColumnRef implements ExprColumnRef {
  private final Expr expr;

  protected ExprColumnRefImpl(String alias, DataType dataType, boolean notNull, Expr expr) {
    super(alias, dataType, notNull);
    this.expr = expr;
  }

  public static ExprColumnRef create(Expr expr, String alias, DataType dataType, boolean notNull) {
    return new ExprColumnRefImpl(alias, dataType, notNull, expr);
  }

  @Override
  public Expr expr() {
    return expr;
  }

  @Override
  public ColumnRef copy() {
    return new ExprColumnRefImpl(alias(), dataType(), notNull(), expr);
  }
}
