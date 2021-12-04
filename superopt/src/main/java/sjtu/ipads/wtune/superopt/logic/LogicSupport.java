package sjtu.ipads.wtune.superopt.logic;

import sjtu.ipads.wtune.superopt.uexpr.UExprTranslationResult;

public interface LogicSupport {
  int EQ = 1, NEQ = -1, UNKNOWN = 0;

  static int proveEq(UExprTranslationResult uExpr) {
    return new LogicProver(uExpr).proveEq();
  }
}
