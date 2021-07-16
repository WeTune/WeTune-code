package sjtu.ipads.wtune.prover.utils;

import static sjtu.ipads.wtune.prover.uexpr.UExpr.Kind.EQ_PRED;

import java.util.List;
import java.util.Objects;
import sjtu.ipads.wtune.prover.uexpr.EqPredTerm;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.Var;

final class TupleCongruence extends CongruenceImpl<Var> {
  static Congruence<Var> make(List<UExpr> predicates) {
    final Congruence<Var> congruence = new TupleCongruence();

    for (UExpr predicate : predicates) {
      if (predicate.kind() != EQ_PRED) continue;
      final EqPredTerm eqTerm = (EqPredTerm) predicate;
      congruence.putCongruent(eqTerm.lhs(), eqTerm.rhs());
    }

    return congruence;
  }

  @Override
  public boolean isCongruent(Var x, Var y) {
    if (super.isCongruent(x, y)) return true;

    if (x.isBase() || y.isBase()) return false;

    // x == y -> x.attr == y.attr
    if (x.isProjected() && y.isProjected())
      return Objects.equals(x.name(), y.name()) && isCongruent(x.base()[0], y.base()[0]);

    // x0 == y0 /\ x1 == y1 ... -> f(x0,x1,...) = f(y0,y1,...)
    if (x.isFunc() && y.isFunc())
      if (Objects.equals(x.name(), y.name())) {
        final Var[] xArgs = x.base(), yArgs = y.base();
        if (xArgs.length != yArgs.length) return false;

        for (int i = 0, bound = xArgs.length; i < bound; ++i)
          if (!isCongruent(xArgs[i], yArgs[i])) return false;

        return true;

      } else {
        return false;
      }

    return false;
  }
}
