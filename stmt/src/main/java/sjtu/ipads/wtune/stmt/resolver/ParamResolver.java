package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.BOOL_EXPR;

/**
 * Mark parameter in a statement.
 *
 * <p>SQLNode of following types may be marked as parameters:
 *
 * <ul>
 *   <li>Literal
 *   <li>ParamMarker
 * </ul>
 */
public class ParamResolver implements SQLVisitor {
  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    final SQLNode ctx = findBoolExprCtx(paramMarker);
    if (ctx != null) findBaseColumn(ctx);
    return false;
  }

  @Override
  public boolean enterLiteral(SQLNode literal) {
    final SQLNode ctx = findBoolExprCtx(literal);
    if (ctx != null) findBaseColumn(ctx);
    return false;
  }

  private static SQLNode findBoolExprCtx(SQLNode startPoint) {
    SQLNode parent = startPoint.parent();
    while (isExpr(parent) && parent.get(BOOL_EXPR) == null) parent = parent.parent();
    if (parent.get(BOOL_EXPR) == null) return null;
    else return parent;
  }

  private static SQLNode findBaseColumn(SQLNode boolExpr) {
    assert boolExpr != null;
    switch (exprKind(boolExpr)) {
      case BINARY:
        {
          if (!boolExpr.get(BOOL_EXPR).isPrimitive()) {
            System.out.println();
          }
        }
      case TERNARY:
        {
          final SQLNode left = boolExpr.get(TERNARY_LEFT);
          return exprKind(left) == Kind.COLUMN_REF ? left : null;
        }
      case EXISTS:
        return null;
      case MATCH:
        {
          final SQLNode col = boolExpr.get(MATCH_COLS).get(0);
          return exprKind(col) == Kind.COLUMN_REF ? col : null;
        }
      case UNARY:
      case COLUMN_REF:
      case LITERAL:
        return null;
      default:
        return assertFalse();
    }
  }
}
