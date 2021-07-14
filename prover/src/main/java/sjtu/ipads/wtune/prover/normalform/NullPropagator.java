package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.utils.Constants.NULL_VAR;

import sjtu.ipads.wtune.prover.uexpr.EqPredTerm;
import sjtu.ipads.wtune.prover.uexpr.TableTerm;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.UninterpretedPredTerm;
import sjtu.ipads.wtune.prover.uexpr.Var;

class NullPropagator {
  static void propagateNull(UExpr c) {
    onNode(c);
  }

  private static void onNode(UExpr expr) {
    for (UExpr child : expr.children()) onNode(child);

    switch (expr.kind()) {
      case TABLE:
        final TableTerm table = (TableTerm) expr;
        if (isNull(table.tuple())) expr.subst(table.tuple(), NULL_VAR);
        break;

      case PRED:
        for (Var var : ((UninterpretedPredTerm) expr).tuple())
          if (isNull(var)) expr.subst(var, NULL_VAR);
        break;

      case EQ_PRED:
        final EqPredTerm eqPred = (EqPredTerm) expr;
        if (isNull(eqPred.left())) eqPred.subst(eqPred.left(), NULL_VAR);
        if (isNull(eqPred.right())) eqPred.subst(eqPred.right(), NULL_VAR);
        break;
    }
  }

  private static boolean isNull(Var var) {
    return var.uses(NULL_VAR);
  }
}
