package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.decision.SimpleDecisionProcedure.decideSame;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.EQ_PRED;
import static sjtu.ipads.wtune.prover.utils.Util.compareTable;
import static sjtu.ipads.wtune.prover.utils.Util.isMatchedUninterpretedPred;
import static sjtu.ipads.wtune.prover.utils.Util.minimizeVars;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import sjtu.ipads.wtune.prover.expr.EqPredTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UninterpretedPredTerm;
import sjtu.ipads.wtune.prover.utils.Congruence;
import sjtu.ipads.wtune.prover.utils.VarAligner;
import sjtu.ipads.wtune.prover.utils.VarAlignment;

class CommonFactorExtractor {
  private final List<Conjunction> terms;
  private final Deque<Conjunction> stack;
  private final List<CommonFactorResult> results;

  private CommonFactorExtractor(List<Conjunction> terms) {
    this.terms = terms;
    this.stack = new LinkedList<>();
    this.results = new ArrayList<>(4);
  }

  static List<CommonFactorResult> extractCommonFactor(List<Conjunction> terms) {
    return new CommonFactorExtractor(terms).extract0();
  }

  private List<CommonFactorResult> extract0() {
    for (int i = 0; i < terms.size(); i++) extract0(0, i, terms.get(i));
    return results;
  }

  private void extract0(int i, int pivotIdx, Conjunction commonFactor) {
    if (i >= terms.size()) {
      makeResult(commonFactor);
      return;
    }

    final Conjunction term = terms.get(i);
    if (i == pivotIdx) {
      stack.push(term);
      extract0(i + 1, pivotIdx, commonFactor);
      stack.pop();
      return;
    }

    final int numVars = Math.min(commonFactor.vars().size(), term.vars().size());

    for (VarAlignment alignment : VarAligner.alignVars(commonFactor, term, numVars)) {
      final Conjunction c0 = alignment.c0(), c1 = alignment.c1();
      final ConjunctionMask intersect0 = new ConjunctionMask(c0);
      final ConjunctionMask intersect1 = new ConjunctionMask(c1);

      intersect(c0, c1, intersect0, intersect1);
      if (intersect0.isEmpty()) continue;

      stack.push(c1);
      extract0(i + 1, pivotIdx, intersect0.getMasked());
      stack.pop();
    }
  }

  private void intersect(Conjunction c0, Conjunction c1, ConjunctionMask m0, ConjunctionMask m1) {
    matchTables(c0.tables(), c1.tables(), m0.tableMask, m1.tableMask);
    matchPreds(c0.preds(), c1.preds(), m0.predMask, m1.predMask);
    m0.negMask = m1.negMask = matchDisjunction(c0.neg(), c1.neg());
    m0.squashMask = m1.squashMask = matchDisjunction(c0.squash(), c1.squash());
  }

  private void matchTables(List<UExpr> ts0, List<UExpr> ts1, boolean[] mask0, boolean[] mask1) {
    assert ts0.size() == mask0.length && ts1.size() == mask1.length;

    for (int i = 0, iBound = ts0.size(); i < iBound; i++)
      for (int j = 0, jBound = ts1.size(); j < jBound; j++)
        if (compareTable(ts0.get(i), ts1.get(j))) {
          mask0[i] = mask1[j] = true;
        }
  }

  private void matchPreds(List<UExpr> ps0, List<UExpr> ps1, boolean[] mask0, boolean[] mask1) {
    assert ps0.size() == mask0.length && ps1.size() == mask1.length;

    final Congruence<Tuple> cong0 = Congruence.make(ps0);
    final Congruence<Tuple> cong1 = Congruence.make(ps1);

    matchEqPreds(ps0, cong1, mask0);
    matchEqPreds(ps1, cong0, mask1);
    matchUninterpretedPreds(ps0, ps1, cong0, cong1, mask0, mask1);
  }

  private boolean matchDisjunction(Disjunction d0, Disjunction d1) {
    return (d0 == null) == (d1 == null) && (d0 == null || decideSame(d0, d1));
  }

  private void matchEqPreds(List<UExpr> ps, Congruence<Tuple> cong, boolean[] mask) {
    assert ps.size() == mask.length;

    for (int i = 0, iBound = ps.size(); i < iBound; i++) {
      final UExpr iPred = ps.get(i);
      if (iPred.kind() != EQ_PRED) continue;

      final EqPredTerm eq = (EqPredTerm) iPred;
      mask[i] = cong.isCongruent(eq.left(), eq.right());
    }
  }

  private void matchUninterpretedPreds(
      List<UExpr> ps0,
      List<UExpr> ps1,
      Congruence<Tuple> cong0,
      Congruence<Tuple> cong1,
      boolean[] mask0,
      boolean[] mask1) {

    for (int i = 0, iBound = ps0.size(); i < iBound; i++) {
      final UExpr iPred = ps0.get(i);
      if (!(iPred instanceof UninterpretedPredTerm)) continue;

      for (int j = 0, jBound = ps1.size(); j < jBound; j++) {
        final UExpr jPred = ps1.get(j);
        if (!(jPred instanceof UninterpretedPredTerm)) continue;

        if (isMatchedUninterpretedPred(iPred, jPred, cong0)
            && isMatchedUninterpretedPred(iPred, jPred, cong1)) {
          mask0[i] = mask1[j] = true;
        }
      }
    }
  }

  private void makeResult(Conjunction commonFactor) {
    final Conjunction factor = minimizeVars(commonFactor.copy());

    final List<Conjunction> result = new ArrayList<>(stack.size());
    for (Conjunction term : stack) {
      final ConjunctionMask m0 = new ConjunctionMask(term);
      final ConjunctionMask m1 = new ConjunctionMask(factor);

      intersect(term, factor, m0, m1);
      result.add(minimizeVars(m0.getComplement()));
    }

    results.add(new CommonFactorResult(factor, result));
  }
}

class CommonFactorResult {
  final Conjunction commonFactor;
  final List<Conjunction> reminders;

  CommonFactorResult(Conjunction commonFactor, List<Conjunction> reminders) {
    this.commonFactor = commonFactor;
    this.reminders = reminders;
  }
}
