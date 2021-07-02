package sjtu.ipads.wtune.prover.decision;

import static sjtu.ipads.wtune.prover.utils.Util.isMatchedUninterpretedPred;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.prover.expr.EqPredTerm;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;
import sjtu.ipads.wtune.prover.expr.UninterpretedPredTerm;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.utils.Congruence;
import sjtu.ipads.wtune.prover.utils.Util;

public final class Minimization {
  // Note: to be called from `DecisionProcedure::sdp`
  public static Conjunction minimize(Conjunction c) {
    final Collection<List<TableTerm>> groups = Util.groupTables(c).values();

    final List<Pair<Tuple, Tuple>> victims = new ArrayList<>();
    for (List<TableTerm> group : groups) {
      if (group.size() <= 1) continue;
      final Tuple pivot = group.get(0).tuple();
      for (TableTerm term : group.subList(1, group.size())) {
        victims.add(Pair.of(pivot, term.tuple()));
      }
    }
    if (victims.isEmpty()) return c;

    c = c.copy();
    for (var pair : victims) c.subst(pair.getValue(), pair.getKey());

    final Set<String> known = new HashSet<>();
    final List<UExpr> tables = new ArrayList<>(c.tables().size());
    for (UExpr table : c.tables()) {
      if (known.add(table.toString())) tables.add(table);
    }
    c.tables().clear();
    c.tables().addAll(tables);

    return c;
  }

  /** Find a matched predicate of `target` in `toMatch`. */
  private static boolean matchTerm(Conjunction toMatch, UExpr target) {
    // For one-shot match. For repeated match, make a Congruence and use the overloaded version.
    return matchTerm(toMatch, target, Congruence.make(toMatch.preds()));
  }

  /** Find a matched predicate of `target` in `toMatch`. */
  private static boolean matchTerm(
      Conjunction toMatch, UExpr target, Congruence<Tuple> congruence) {
    // for predicate, we just consult the congruence
    if (target.kind() == Kind.EQ_PRED) {
      final EqPredTerm eqPred = (EqPredTerm) target;
      return congruence.isCongruent(eqPred.left(), eqPred.right());
    }

    // for uninterpreted predicate, we match the literal
    final UninterpretedPredTerm p0 = (UninterpretedPredTerm) target;
    for (UExpr expr : toMatch.preds()) {
      if (expr.kind() != Kind.PRED) continue;

      final UninterpretedPredTerm p1 = (UninterpretedPredTerm) expr;
      if (isMatchedUninterpretedPred(p0, p1, congruence)) return true;
    }

    return false;
  }
}
