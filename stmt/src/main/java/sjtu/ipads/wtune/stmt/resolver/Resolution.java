package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.stmt.Statement;

public interface Resolution {
  static void resolveBoolExpr(Statement stmt) {
    ResolveBoolExpr.resolve(stmt);
  }

  static void resolveQueryScope(Statement stmt) {
    ResolveQueryScope.resolve(stmt);
  }

  static void resolveTable(Statement stmt) {
    ResolveTable.resolve(stmt);
  }

  static void resolveColumnRef(Statement stmt) {
    ResolveColumnRef.resolve(stmt);
  }

  static void resolveJoinCondition(Statement stmt) {
    ResolveJoinCondition.resolve(stmt);
  }

  static void resolveParamSimple(Statement stmt) {
    ResolveParamSimple.resolve(stmt);
  }

  static void resolveParamFull(Statement stmt) {
    ResolveParamFull.resolve(stmt);
  }
}
