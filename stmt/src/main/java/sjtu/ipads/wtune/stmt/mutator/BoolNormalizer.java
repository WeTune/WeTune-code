package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.resovler.IdResolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_HAVING;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.JOINED_ON;

/**
 * Replace single boolean column by binary op.
 *
 * <p>e.g. `a` AND xxx => `a` IS TRUE AND xxx
 */
public class BoolNormalizer implements SQLVisitor, Mutator {
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
    final Kind kind = exprKind(expr);
    if (kind == Kind.COLUMN_REF) {
      final SQLNode node = binary(expr.copy(), literal(LiteralType.BOOL, true), BinaryOp.IS);
      expr.replaceThis(node);
      expr.relinkAll();
      IdResolver.resolve(node);

    } else if (kind == Kind.BINARY && expr.get(BINARY_OP) == BinaryOp.IS) {
      final SQLNode right = expr.get(BINARY_RIGHT);
      if (exprKind(right) == Kind.LITERAL
          && right.get(LITERAL_TYPE) == LiteralType.BOOL
          && right.get(LITERAL_VALUE).equals(false)) {
        handleExpr(expr.get(BINARY_LEFT));

        expr.replaceThis(unary(expr.get(BINARY_LEFT), UnaryOp.NOT));
        expr.relinkAll();
        IdResolver.resolve(expr);
      }

    } else if (kind == Kind.BINARY && expr.get(BINARY_OP).isLogic()) {
      handleExpr(expr.get(BINARY_LEFT));
      handleExpr(expr.get(BINARY_RIGHT));

    } else if (kind == Kind.UNARY && expr.get(UNARY_OP).isLogic()) {
      handleExpr(expr.get(UNARY_EXPR));
    }
    assert kind == Kind.UNARY
        || kind == Kind.BINARY
        || kind == Kind.TERNARY
        || kind == Kind.EXISTS
        || kind == Kind.MATCH
        || kind == Kind.COLUMN_REF;
  }

  @Override
  public void mutate(Statement stmt) {
    stmt.parsed().accept(this);
  }
}
