package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

public interface BoolExprManager {
  FieldKey<BoolExpr> BOOL_EXPR = BoolExprManagerImpl.fieldKey();

  BoolExpr bool(ASTNode node);

  BoolExpr setBool(ASTNode node, BoolExpr expr);

  static BoolExprManager build() {
    return BoolExprManagerImpl.build();
  }
}
