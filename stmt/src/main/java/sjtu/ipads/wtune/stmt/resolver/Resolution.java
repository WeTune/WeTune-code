package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

public interface Resolution {
  static Params resolveParamSimple(ASTNode node) {
    return ParamsImpl.simpleBuild(node);
  }

  static Params resolveParamFull(ASTNode node) {
    return ParamsImpl.build(node);
  }

  static BoolExprManager resolveBoolExpr(ASTNode node) {
    return ResolveBoolExpr.resolve(node);
  }
}
