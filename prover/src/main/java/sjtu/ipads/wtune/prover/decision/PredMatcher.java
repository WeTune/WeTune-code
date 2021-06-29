package sjtu.ipads.wtune.prover.decision;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.EQ_PRED;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.PRED;

import java.util.List;
import java.util.Objects;
import sjtu.ipads.wtune.prover.expr.EqPredTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UninterpretedPredTerm;
import sjtu.ipads.wtune.prover.utils.Congruence;

public class PredMatcher extends Matcher<UExpr> {
  private final Congruence<Tuple> xCongruence, yCongruence;

  protected PredMatcher(List<UExpr> xs, List<UExpr> ys) {
    super(xs, ys);
    xCongruence = Congruence.make(xs);
    yCongruence = Congruence.make(ys);
  }

  @Override
  boolean tryMatch(UExpr x, UExpr y) {
    if (x.kind() == EQ_PRED && y.kind() == EQ_PRED) {
      // For EqPred, we don't actually do matching.
      // Instead, we check whether the eq relation is implied by the counterpart's congruence.
      final EqPredTerm xEq = (EqPredTerm) x;
      final EqPredTerm yEq = (EqPredTerm) y;
      return yCongruence.isCongruent(xEq.left(), xEq.right())
          && xCongruence.isCongruent(yEq.left(), yEq.right());
    }

    if (x.kind() == PRED && y.kind() == PRED) {
      final UninterpretedPredTerm xPred = (UninterpretedPredTerm) x;
      final UninterpretedPredTerm yPred = (UninterpretedPredTerm) y;

      // x0 == y0 /\ x1 == y1 /\ ... -> pred(x0,x1,...) == pred(y0,y1,...)
      if (Objects.equals(xPred.name(), yPred.name())) {
        final Tuple[] xArgs = xPred.tuple(), yArgs = yPred.tuple();
        if (xArgs.length != yArgs.length) {
          return false;
        }

        for (int i = 0, bound = xArgs.length; i < bound; ++i) {
          if (!xCongruence.isCongruent(xArgs[i], yArgs[i])
              || !yCongruence.isCongruent(xArgs[i], yArgs[i])) {
            return false;
          }
        }

        return true;

      } else {
        return false;
      }
    }

    return false;
  }
}
