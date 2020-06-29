package sjtu.ipads.wtune.stmt.resovler;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.BoolExpr;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_HAVING;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.JOINED_ON;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.BOOL_EXPR;

public class BoolPrimitiveResolver implements Resolver, SQLVisitor {
  @Override
  public boolean enterCase(SQLNode _case) {
    // ignore the form CASE cond WHEN val0 THEN ... END,
    // because val0 is not boolean
    return _case.get(CASE_COND) == null;
  }

  @Override
  public boolean enterWhen(SQLNode when) {
    handleExpr(when.get(WHEN_COND));
    return false;
  }

  @Override
  public boolean enterChild(Attrs.Key<SQLNode> key, SQLNode child) {
    if (key == JOINED_ON || key == QUERY_SPEC_WHERE || key == QUERY_SPEC_HAVING) {
      handleExpr(child);
      return false;
    }
    return true;
  }

  private void handleExpr(SQLNode expr) {
    if (expr == null) return;
    assert expr.type() == SQLNode.Type.EXPR;
    // `expr` must be evaluated as boolean

    final BoolExpr boolExpr = new BoolExpr();
    boolExpr.setPrimitive(false);
    boolExpr.setNode(expr);

    final Kind kind = exprKind(expr);

    if (kind == Kind.BINARY && expr.get(BINARY_OP).isLogic()) {
      handleExpr(expr.get(BINARY_LEFT));
      handleExpr(expr.get(BINARY_RIGHT));

    } else if (kind == Kind.UNARY && expr.get(UNARY_OP).isLogic()) {
      handleExpr(expr.get(UNARY_EXPR));

    } else boolExpr.setPrimitive(true);

    expr.put(BOOL_EXPR, boolExpr);
  }

  @Override
  public void resolve(Statement stmt) {
    stmt.parsed().accept(this);
  }
}
