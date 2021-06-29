package sjtu.ipads.wtune.prover.decision;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static sjtu.ipads.wtune.prover.decision.SimpleDecisionProcedure.decideSame;
import static sjtu.ipads.wtune.prover.utils.PermutationIter.permute;
import static sjtu.ipads.wtune.prover.utils.Util.compareTable;
import static sjtu.ipads.wtune.prover.utils.Util.isMatchedUninterpretedPred;

import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.prover.expr.EqPredTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;
import sjtu.ipads.wtune.prover.expr.UninterpretedPredTerm;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.utils.Congruence;
import sjtu.ipads.wtune.prover.utils.Util;

/**
 * Eliminate tautology by excluded middle.
 *
 * <p>i.e., Sum(not(T1 + T2 + ...) * E) + Sum(T1 * E) + Sum(T2 * E) + ... => Sum(E)
 */
class TautologyEliminator {
  public static Disjunction eliminate(Disjunction d) {
    for (Conjunction c : d) {
      final CollapsableFinder finder = new CollapsableFinder(c, d.conjunctions());
      if (!finder.find()) continue;

      final Conjunction e = finder.getE();
      final TIntCollection group = finder.getGroup();

      final List<Conjunction> newConjunctions = new ArrayList<>();
      for (int i = 0; i < d.conjunctions().size(); i++)
        if (!group.contains(i)) newConjunctions.add(d.conjunctions().get(i));
      newConjunctions.add(e);

      return eliminate(Disjunction.make(newConjunctions));
    }

    return d;
  }

  // find a `E` such that c0 = `E` * T1 and c1 = `E` * T2
  private static boolean intersect(
      Conjunction c0, Conjunction c1, ConjunctionMask intersect0, ConjunctionMask intersect1) {
    /* 1. Tables */
    final boolean[] tableMasks0 = intersect0.maskedTables;
    final boolean[] tableMasks1 = intersect1.maskedTables;
    boolean tableMatched = false;

    for (int i = 0, iBound = c0.tables().size(); i < iBound; i++) {
      final UExpr iTable = c0.tables().get(i);
      for (int j = 0, jBound = c1.tables().size(); j < jBound; j++) {
        final UExpr jTable = c1.tables().get(j);
        if (compareTable(iTable, jTable)) {
          tableMasks0[i] = true;
          tableMasks1[j] = true;
          tableMatched = true;
        }
      }
    }

    /* 2. Predicates */
    final Congruence<Tuple> cong0 = Congruence.make(c0.predicates());
    final Congruence<Tuple> cong1 = Congruence.make(c1.predicates());
    final boolean[] predMasks0 = intersect0.maskedPredicates;
    final boolean[] predMasks1 = intersect1.maskedPredicates;
    boolean predMatched = false;

    for (int i = 0, iBound = c0.predicates().size(); i < iBound; i++) {
      final UExpr iPred = c0.predicates().get(i);
      if (iPred.kind() == Kind.EQ_PRED) {
        final EqPredTerm eqPred = (EqPredTerm) iPred;
        if (cong0.isCongruent(eqPred.left(), eqPred.right())
            && cong1.isCongruent(eqPred.left(), eqPred.right())) predMatched = predMasks0[i] = true;
      }
    }

    for (int j = 0, jBound = c1.predicates().size(); j < jBound; j++) {
      final UExpr jPred = c1.predicates().get(j);
      if (jPred.kind() == Kind.EQ_PRED) {
        final EqPredTerm eqPred = (EqPredTerm) jPred;
        if (cong0.isCongruent(eqPred.left(), eqPred.right())
            && cong1.isCongruent(eqPred.left(), eqPred.right())) predMatched = predMasks1[j] = true;
      }
    }

    for (int i = 0, iBound = c0.predicates().size(); i < iBound; i++) {
      final UExpr iPred = c0.predicates().get(i);
      if (!(iPred instanceof UninterpretedPredTerm)) continue;

      for (int j = 0, jBound = c1.predicates().size(); j < jBound; j++) {
        final UExpr jPred = c1.predicates().get(j);
        if (!(jPred instanceof UninterpretedPredTerm)) continue;

        if (isMatchedUninterpretedPred(iPred, jPred, cong0)
            && isMatchedUninterpretedPred(iPred, jPred, cong1)) {
          predMatched = true;
          predMasks0[i] = true;
          predMasks1[j] = true;
        }
      }
    }

    /* 3. Negation */
    final Disjunction negC = c0.negation(), negE = c1.negation();
    final boolean negMatched =
        intersect0.maskedNegation =
            intersect1.maskedNegation =
                (negC == null) == (negE == null) && (negC == null || decideSame(negC, negE));

    /* 4. Squash */
    final Disjunction sqC = c0.squash(), sqE = c1.squash();
    final boolean sqMatched =
        intersect0.maskedSquash =
            intersect1.maskedSquash =
                (sqC == null) == (sqE == null) && (sqC == null || decideSame(sqC, sqE));

    return tableMatched || predMatched || negMatched || sqMatched;
  }

  // This method check whether `c` is in from (`diff` * `E`)
  private static boolean minus(Conjunction c, Conjunction E, ConjunctionMask intersect0) {
    final ConjunctionMask intersect1 = new ConjunctionMask(E);
    return intersect(c, E, intersect0, intersect1) && intersect1.isFullMasked();
  }

  private static class CollapsableFinder {
    /*
     Given a conjunction `pivot` and a set of conjunctions `terms` {T1, T2, ... Tn},
     find a subset of `terms` such that
       pivot = E * X0, Ti = E * X1, Tj = E * X2, ..., Tk = E * Xk
       where X0 + X1 + X2 + ... Xk always greater than 0.
     We call such subset "Collapsable Group"
    */

    private final Conjunction pivot;
    private final List<Conjunction> terms;

    private TIntList collapsableGroup;
    private Conjunction E;

    private CollapsableFinder(Conjunction pivot, List<Conjunction> terms) {
      this.pivot = pivot;
      this.terms = terms;
    }

    boolean find() {
      E = null;
      collapsableGroup = new TIntArrayList();

      return find(0, null, new TautologyInference(pivot.vars()));
    }

    Conjunction getE() {
      return E;
    }

    TIntList getGroup() {
      return collapsableGroup;
    }

    private boolean find(int i, Conjunction E, TautologyInference infer) {
      /*
       Briefly, we iterate over each `Tk`, try to chop the `E` and accumulate the residual `Xk`.
       At last, we check whether the sum of `Xk` is greater than 0.

       Details:
       1. Each `Tx` may contain its own bounded vars. Different alignment may yield different result.
          So we have to try them all.
       2. `E` is determined by the common part between `pivot` and `Ti`, and then fixed for following `Tk`.
       3. Some `Tk` may not contains `E`. They are just skipped.
      */
      if (i >= terms.size()) {
        if (infer.checkTautology()) {
          this.E = E;
          return true;
        }
        return false;
      }

      final Conjunction term = terms.get(i);
      if (term == pivot) return find(i + 1, E, infer); // skip pivot itself.

      /* 1. Align the variables. */
      // Try each embedding of the pivot's vars into the term to check.
      // See #embedBoundedVars for detail.
      final List<Tuple> vars = pivot.vars();
      final int numVars0 = vars.size(), numVars1 = term.vars().size();
      boolean skipToNext = true;

      for (int[] permutation : permute(max(numVars0, numVars1), min(numVars0, numVars1))) {
        // perform the embedding
        final Conjunction substTerm = Util.embedBoundedVars(term, vars, permutation);
        /* 2. Check if `term` is in the form `E` * `diff`. */
        final ConjunctionMask intersect0 = new ConjunctionMask(substTerm);
        final ConjunctionMask intersect1 = new ConjunctionMask(pivot);
        final boolean isHead = E == null;

        if (isHead) {
          /* 2.1. `E` is not fixed. (This must be the 1st term encountered) */
          // "intersect" the `pivot` and `term`.
          // Get the intersection and the differences at `term` and `pivot` side.
          if (!intersect(substTerm, pivot, intersect0, intersect1)) continue;

          E = intersect1.getMasked();
        } else {
          /* 2.2. `E` is fixed. */
          // "minus" the `E` from the `term` and get the residual part.
          if (!minus(substTerm, E, intersect0)) continue;
        }

        /* 3. Accumulate the `diff` part */
        final TautologyInference inferCopy = infer.copy();
        inferCopy.add(intersect0.getComplement());
        if (isHead) inferCopy.add(intersect1.getComplement());

        /* 4. `term` is a participant. Remember it and return. */
        // If the `term` contains `E`, it's impossible to form a collapsable group without `term`.
        // So we needn't skip to next in this case.
        skipToNext = false;
        if (find(i + 1, E, inferCopy)) {
          collapsableGroup.add(i);
          return true;
        }
      }

      /* 5. Nope, this term is not a participant. Skip this and goto next. */
      return skipToNext && find(i + 1, E, infer);
    }
  }

  private static class ConjunctionMask {
    private final Conjunction c;
    private final boolean[] maskedTables, maskedPredicates;
    private boolean maskedNegation, maskedSquash;

    private ConjunctionMask(Conjunction c) {
      this.c = c;
      this.maskedTables = new boolean[c.tables().size()];
      this.maskedPredicates = new boolean[c.predicates().size()];
    }

    Conjunction getMasked() {
      final List<UExpr> tables = c.tables();
      final List<UExpr> complementTables = new ArrayList<>(tables.size());
      for (int i = 0, bound = tables.size(); i < bound; i++)
        if (maskedTables[i]) complementTables.add(tables.get(i));

      final List<UExpr> preds = c.predicates();
      final List<UExpr> complementPreds = new ArrayList<>(preds.size());
      for (int i = 0, bound = preds.size(); i < bound; i++)
        if (maskedTables[i]) complementPreds.add(preds.get(i));

      final Disjunction squash = maskedSquash ? c.squash() : null;
      final Disjunction negation = maskedNegation ? c.negation() : null;

      final Conjunction newConj =
          Conjunction.make(c.vars(), complementTables, complementPreds, squash, negation);
      Util.minimizeVars(newConj);

      return newConj;
    }

    Conjunction getComplement() {
      final List<UExpr> tables = c.tables();
      final List<UExpr> complementTables = new ArrayList<>(tables.size());
      for (int i = 0, bound = tables.size(); i < bound; i++)
        if (!maskedTables[i]) complementTables.add(tables.get(i));

      final List<UExpr> preds = c.predicates();
      final List<UExpr> complementPreds = new ArrayList<>(preds.size());
      for (int i = 0, bound = preds.size(); i < bound; i++)
        if (!maskedTables[i]) complementPreds.add(preds.get(i));

      final Disjunction squash = maskedSquash ? null : c.squash();
      final Disjunction negation = maskedNegation ? null : c.negation();

      final Conjunction newConj =
          Conjunction.make(c.vars(), complementTables, complementPreds, squash, negation);
      Util.minimizeVars(newConj);

      return newConj;
    }

    boolean isFullMasked() {
      for (boolean m : maskedTables) if (!m) return false;
      for (boolean m : maskedPredicates) if (!m) return false;
      return maskedNegation && maskedSquash;
    }
  }
}
