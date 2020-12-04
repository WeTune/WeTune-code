package sjtu.ipads.wtune.solver.sql;

import sjtu.ipads.wtune.solver.sql.expr.Expr;

public interface ExprColumnRef extends ColumnRef {
  Expr expr();
}
