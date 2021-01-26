package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.BoolExpr;
import sjtu.ipads.wtune.stmt.collector.BoolCollector;
import sjtu.ipads.wtune.stmt.Statement;

import static sjtu.ipads.wtune.sqlparser.ast.ExprAttr.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.BINARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.UNARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.BOOL_EXPR;

class ResolveBoolExpr {
  public static void resolve(Statement stmt) {
    BoolCollector.collect(stmt.parsed()).forEach(ResolveBoolExpr::resolveBool);
  }

  private static void resolveBool(SQLNode expr) {
    assert EXPR.isInstance(expr);
    // `expr` must be evaluated as boolean

    final BoolExpr boolExpr = new BoolExpr();
    boolExpr.setPrimitive(false);
    boolExpr.setNode(expr);

    if (BINARY.isInstance(expr) && expr.get(BINARY_OP).isLogic()) {
      resolveBool(expr.get(BINARY_LEFT));
      resolveBool(expr.get(BINARY_RIGHT));

    } else if (UNARY.isInstance(expr) && expr.get(UNARY_OP).isLogic()) {
      resolveBool(expr.get(UNARY_EXPR));

    } else boolExpr.setPrimitive(true);

    expr.put(BOOL_EXPR, boolExpr);
  }
}
