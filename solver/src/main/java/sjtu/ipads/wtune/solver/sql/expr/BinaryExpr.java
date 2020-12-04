package sjtu.ipads.wtune.solver.sql.expr;

import sjtu.ipads.wtune.solver.sql.Operator;

public interface BinaryExpr extends Expr {
  Expr left();

  Expr right();

  Operator operator();
}
