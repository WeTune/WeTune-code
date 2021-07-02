package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.common.utils.FuncUtils.any;
import static sjtu.ipads.wtune.prover.normalform.CommonFactorExtractor.extractCommonFactor;
import static sjtu.ipads.wtune.prover.utils.Util.arrange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.math3.util.Combinations;

class ExcludedMiddle {
  static Disjunction collapse(Disjunction d) {
    return collapse(d, false);
  }

  static Disjunction collapse(Disjunction d, boolean asBool) {
    if (d == null) return null;

    final ListIterator<Conjunction> iter = d.conjunctions().listIterator();
    while (iter.hasNext()) {
      final Conjunction c = iter.next();
      final Disjunction sq = c.squash(), neg = c.neg();
      final Disjunction newSq = collapse(sq, true);
      final Disjunction newNeg = collapse(neg, true);
      if (newSq == sq && newNeg == neg) continue;

      iter.set(Conjunction.make(c.vars(), c.tables(), c.preds(), newSq, newNeg));
    }

    final List<Conjunction> conjunctions = d.conjunctions();
    if (conjunctions.size() <= 1) return d;

    for (int i = conjunctions.size(); i >= 2; --i)
      for (int[] indices : new Combinations(conjunctions.size(), i))
        for (CommonFactorResult result : extractCommonFactor(arrange(conjunctions, indices))) {

          if (!asBool && any(result.reminders, it -> !it.tables().isEmpty())) continue;

          final TautologySolver solver = new TautologySolver(result.commonFactor.vars());
          result.reminders.forEach(solver::add);

          if (solver.solve())
            return collapse(makeCollapsed(conjunctions, indices, result.commonFactor), asBool);
        }

    return d;
  }

  private static Disjunction makeCollapsed(
      List<Conjunction> terms, int[] removed, Conjunction collapsed) {
    final List<Conjunction> newTerms = new ArrayList<>(terms.size() - removed.length);
    for (int i = 0; i < terms.size(); i++) {
      if (Arrays.binarySearch(removed, i) < 0) newTerms.add(terms.get(i));
    }
    newTerms.add(collapsed);
    return Disjunction.make(newTerms);
  }
}
