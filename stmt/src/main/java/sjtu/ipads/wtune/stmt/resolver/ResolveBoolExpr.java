package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_HAVING;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_ON;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.UNARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.stmt.resolver.BoolExprManager.BOOL_EXPR;

class ResolveBoolExpr implements ASTVistor {
  public static BoolExprManager resolve(ASTNode node) {
    if (node.manager(BoolExprManager.class) == null)
      node.context().addManager(BoolExprManager.class, BoolExprManager.build());

    node.accept(new ResolveBoolExpr());
    return node.manager(BoolExprManager.class);
  }

  @Override
  public boolean enterCase(ASTNode _case) {
    // ignore the form CASE cond WHEN val0 THEN ... END,
    // because val0 is not boolean
    return _case.get(CASE_COND) == null;
  }

  @Override
  public boolean enterWhen(ASTNode when) {
    if (when != null) resolveBool(when.get(WHEN_COND));
    return false;
  }

  @Override
  public boolean enterChild(ASTNode parent, FieldKey<ASTNode> key, ASTNode child) {
    if (child != null
        && (key == JOINED_ON || key == QUERY_SPEC_WHERE || key == QUERY_SPEC_HAVING)) {
      resolveBool(child);
      return false;
    }
    return true;
  }

  private static void resolveBool(ASTNode expr) {
    assert EXPR.isInstance(expr);
    // `expr` must be evaluated as boolean

    final BoolExpr boolExpr = new BoolExpr();
    boolExpr.setPrimitive(false);

    if (BINARY.isInstance(expr) && expr.get(BINARY_OP).isLogic()) {
      resolveBool(expr.get(BINARY_LEFT));
      resolveBool(expr.get(BINARY_RIGHT));

    } else if (UNARY.isInstance(expr) && expr.get(UNARY_OP).isLogic()) {
      resolveBool(expr.get(UNARY_EXPR));

    } else boolExpr.setPrimitive(true);

    expr.set(BOOL_EXPR, boolExpr);
  }
}
