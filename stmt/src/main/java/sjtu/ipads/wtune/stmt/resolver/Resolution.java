package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

public interface Resolution {
  static ParamManager resolveParamSimple(SQLNode node) {
    return ResolveParamSimple.resolve(node);
  }

  static ParamManager resolveParamFull(SQLNode node) {
    return ResolveParamFull.resolve(node);
  }

  static BoolExprManager resolveBoolExpr(SQLNode node) {
    return ResolveBoolExpr.resolve(node);
  }
}
