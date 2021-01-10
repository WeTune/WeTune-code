package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.sqlparser.ast.constants.UnaryOp;
import sjtu.ipads.wtune.stmt.collector.BoolCollector;

import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.EXPR_KIND;
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
      final SQLNode trueLiteral = SQLNode.simple(LITERAL);
      trueLiteral.put(LITERAL_TYPE, LiteralType.BOOL);
      trueLiteral.put(LITERAL_VALUE, true);

      final SQLNode binary = SQLNode.simple(BINARY);
      binary.put(BINARY_LEFT, SQLNode.simple(expr));
      binary.put(BINARY_OP, BinaryOp.IS);
      binary.put(BINARY_RIGHT, trueLiteral);

      expr.update(binary);

    } else if (expr.get(BINARY_OP) == BinaryOp.IS) {
      final SQLNode right = expr.get(BINARY_RIGHT);
      if (LITERAL.isInstance(right)
          && right.get(LITERAL_TYPE) == LiteralType.BOOL
          && right.get(LITERAL_VALUE).equals(Boolean.FALSE)) {

        normalizeExpr(expr.get(BINARY_LEFT));

        final SQLNode left = expr.get(BINARY_LEFT);
        expr.directAttrs().clear();
        expr.put(EXPR_KIND, UNARY);
        expr.put(UNARY_OP, UnaryOp.NOT);
        expr.put(UNARY_EXPR, left);
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
