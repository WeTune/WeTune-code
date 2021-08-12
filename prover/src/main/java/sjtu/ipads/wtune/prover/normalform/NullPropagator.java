package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.uexpr.*;

import static sjtu.ipads.wtune.prover.utils.Constants.NULL;
import static sjtu.ipads.wtune.prover.utils.UExprUtils.isNull;

// Should be called after Canonization::applyConst.
// Substitute "NULL.attr" as "NULL"
class NullPropagator {
  static void propagateNull(UExpr c) {
    onNode(c);
  }

  private static void onNode(UExpr expr) {
    for (UExpr child : expr.children()) onNode(child);

    switch (expr.kind()) {
      case TABLE:
        final TableTerm table = (TableTerm) expr;
        if (isNull(table.var())) expr.subst(table.var(), NULL);
        break;

      case PRED:
        for (Var var : ((UninterpretedPredTerm) expr).vars())
          if (isNull(var)) expr.subst(var, NULL);
        break;

      case EQ_PRED:
        final EqPredTerm eqPred = (EqPredTerm) expr;
        if (isNull(eqPred.lhs())) eqPred.subst(eqPred.lhs(), NULL);
        if (isNull(eqPred.rhs())) eqPred.subst(eqPred.rhs(), NULL);
        break;
    }
  }
}
