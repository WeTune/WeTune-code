package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

public interface Resolution {
  static ParamManager resolveParamSimple(ASTNode node) {
    return ResolveParamSimple.resolve(node);
  }

  static ParamManager resolveParamFull(ASTNode node) {
    return ResolveParamFull.resolve(node);
  }

  static BoolExprManager resolveBoolExpr(ASTNode node) {
    return ResolveBoolExpr.resolve(node);
  }
}
