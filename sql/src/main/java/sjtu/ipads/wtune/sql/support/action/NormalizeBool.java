package sjtu.ipads.wtune.sql.support.action;

import sjtu.ipads.wtune.sql.ast.ExprKind;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;

import static sjtu.ipads.wtune.sql.SqlSupport.*;
import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast.SqlKind.Expr;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.Expr_Kind;
import static sjtu.ipads.wtune.sql.ast.constants.BinaryOpKind.EQUAL;
import static sjtu.ipads.wtune.sql.ast.constants.BinaryOpKind.IS;
import static sjtu.ipads.wtune.sql.ast.constants.LiteralKind.BOOL;
import static sjtu.ipads.wtune.sql.ast.constants.UnaryOpKind.NOT;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.predicateLocator;

class NormalizeBool {
  static void normalize(SqlNode node) {
    for (SqlNode target : predicateLocator().gather(node)) normalizeExpr(target);
  }

  private static void normalizeExpr(SqlNode expr) {
    assert Expr.isInstance(expr);
    // `expr` must be evaluated as boolean

    final SqlContext ctx = expr.context();

    if (ColRef.isInstance(expr)) {
      final SqlNode lhs = copyAst(expr).go();
      final SqlNode rhs = mkLiteral(ctx, BOOL, true);
      final SqlNode binary = mkBinary(ctx, EQUAL, lhs, rhs);
      ctx.displaceNode(expr.nodeId(), binary.nodeId());

    } else if (expr.$(Binary_Op) == IS) {
      final SqlNode rhs = expr.$(Binary_Right);

      if (Literal.isInstance(rhs) && rhs.$(Literal_Kind) == BOOL) {
        expr.$(Binary_Op, EQUAL);

        if (rhs.$(Literal_Value).equals(Boolean.FALSE)) {
          normalizeExpr(expr.$(Binary_Left));

          final SqlNode operand = copyAst(expr.$(Binary_Left)).go();
          final SqlNode unary = mkUnary(ctx, NOT, operand);
          ctx.displaceNode(expr.nodeId(), unary.nodeId());
        }
      }

    } else if (Unary.isInstance(expr) && expr.$(Unary_Op).isLogic()) {
      normalizeExpr(expr.$(Unary_Expr));

    } else if (Binary.isInstance(expr) && expr.$(Binary_Op).isLogic()) {
      normalizeExpr(expr.$(Binary_Left));
      normalizeExpr(expr.$(Binary_Right));
    }

    final ExprKind exprKind = expr.$(Expr_Kind);
    assert exprKind == Unary
        || exprKind == Binary
        || exprKind == Ternary
        || exprKind == Exists
        || exprKind == Match
        || exprKind == ColRef
        || exprKind == Literal;
  }
}
