package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.sqlparser.ast.constants.UnaryOp;
import sjtu.ipads.wtune.stmt.utils.BoolCollector;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;

class NormalizeBool {
  public static SQLNode normalize(SQLNode node) {
    BoolCollector.collect(node).forEach(NormalizeBool::normalizeExpr);
    return node;
  }

  private static void normalizeExpr(SQLNode expr) {
    assert EXPR.isInstance(expr);
    // `expr` must be evaluated as boolean

    if (COLUMN_REF.isInstance(expr)) {
      final SQLNode trueLiteral = SQLNode.expr(LITERAL);
      trueLiteral.set(LITERAL_TYPE, LiteralType.BOOL);
      trueLiteral.set(LITERAL_VALUE, true);

      final SQLNode binary = SQLNode.expr(BINARY);
      binary.set(BINARY_LEFT, expr.copy());
      binary.set(BINARY_OP, BinaryOp.IS);
      binary.set(BINARY_RIGHT, trueLiteral);

      expr.update(binary);

    } else if (expr.get(BINARY_OP) == BinaryOp.IS) {
      final SQLNode right = expr.get(BINARY_RIGHT);
      if (LITERAL.isInstance(right)
          && right.get(LITERAL_TYPE) == LiteralType.BOOL
          && right.get(LITERAL_VALUE).equals(Boolean.FALSE)) {

        normalizeExpr(expr.get(BINARY_LEFT));

        final SQLNode unary = SQLNode.expr(UNARY);
        unary.set(UNARY_OP, UnaryOp.NOT);
        unary.set(UNARY_EXPR, expr.get(BINARY_LEFT));

        expr.update(unary);
      }

    } else if (BINARY.isInstance(expr) && expr.get(BINARY_OP).isLogic()) {
      normalizeExpr(expr.get(BINARY_LEFT));
      normalizeExpr(expr.get(BINARY_RIGHT));

    } else if (UNARY.isInstance(expr) && expr.get(UNARY_OP).isLogic())
      normalizeExpr(expr.get(UNARY_EXPR));

    final ExprType exprKind = expr.get(EXPR_KIND);
    assert exprKind == UNARY
        || exprKind == BINARY
        || exprKind == TERNARY
        || exprKind == EXISTS
        || exprKind == MATCH
        || exprKind == COLUMN_REF
        || exprKind == LITERAL;
  }
}