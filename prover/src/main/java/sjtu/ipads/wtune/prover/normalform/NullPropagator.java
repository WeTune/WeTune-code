package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.utils.Constants.NULL_TUPLE;

import sjtu.ipads.wtune.prover.expr.EqPredTerm;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UninterpretedPredTerm;

class NullPropagator {
  static void propagateNull(UExpr c) {
    onNode(c);
  }

  private static void onNode(UExpr expr) {
    for (UExpr child : expr.children()) onNode(child);

    switch (expr.kind()) {
      case TABLE:
        final TableTerm table = (TableTerm) expr;
        if (isNull(table.tuple())) expr.subst(table.tuple(), NULL_TUPLE);
        break;

      case PRED:
        for (Tuple tuple : ((UninterpretedPredTerm) expr).tuple())
          if (isNull(tuple)) expr.subst(tuple, NULL_TUPLE);
        break;

      case EQ_PRED:
        final EqPredTerm eqPred = (EqPredTerm) expr;
        if (isNull(eqPred.left())) eqPred.subst(eqPred.left(), NULL_TUPLE);
        if (isNull(eqPred.right())) eqPred.subst(eqPred.right(), NULL_TUPLE);
        break;
    }
  }

  private static boolean isNull(Tuple tuple) {
    return tuple.uses(NULL_TUPLE);
  }
}
