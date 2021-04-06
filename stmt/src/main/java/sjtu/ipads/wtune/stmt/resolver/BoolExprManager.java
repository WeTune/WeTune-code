package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.AttributeManager;

public interface BoolExprManager extends AttributeManager<BoolExpr> {
  FieldKey<BoolExpr> BOOL_EXPR = BoolExprManagerImpl.field();

  BoolExpr bool(ASTNode node);

  BoolExpr setBool(ASTNode node, BoolExpr expr);

  static BoolExprManager build() {
    return new BoolExprManagerImpl();
  }
}
