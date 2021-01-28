package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.resolver.internal.BoolExprManagerImpl;

public interface BoolExprManager {
  FieldKey<BoolExpr> BOOL_EXPR = BoolExprManagerImpl.fieldKey();

  BoolExpr bool(SQLNode node);

  BoolExpr setBool(SQLNode node, BoolExpr expr);

  static BoolExprManager build() {
    return BoolExprManagerImpl.build();
  }
}
