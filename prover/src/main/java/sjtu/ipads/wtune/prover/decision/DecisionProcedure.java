package sjtu.ipads.wtune.prover.decision;

import static java.lang.Math.max;
import static sjtu.ipads.wtune.prover.decision.Minimization.minimize;
import static sjtu.ipads.wtune.prover.utils.Constants.DECISION_VAR_PREFIX;
import static sjtu.ipads.wtune.prover.utils.VarAligner.alignVars;

import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.prover.DecisionContext;
import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.utils.Util;
import sjtu.ipads.wtune.prover.utils.VarAlignment;

public final class DecisionProcedure {
  private final DecisionContext ctx;
  private final Disjunction x, y;
  private final ProofHelper helper;

  DecisionProcedure(DecisionContext ctx, Disjunction x, Disjunction y) {
    this.ctx = ctx;
    this.x = x;
    this.y = y;
    this.helper = new ProofHelper(ctx);
  }

  public static Proof decide(Disjunction x, Disjunction y, DecisionContext ctx) {
    return new DecisionProcedure(ctx, x, y).decide();
  }

  public Proof decide() {
    return udp(x, y);
  }

  private Proof udp(Disjunction x, Disjunction y) {
    final TermBijectionMatcher matcher =
        new TermBijectionMatcher(x.conjunctions(), y.conjunctions());
    if (!matcher.match()) return null;

    return helper.proveDisjunctionEqBag(x, y, matcher.proofs());
  }

  private Proof tdp(Conjunction x, Conjunction y) {
    if (x.vars().size() != y.vars().size()) return null;
    if (x.neg() == null ^ y.neg() == null) return null;
    if (x.squash() == null ^ y.squash() == null) return null;
    if (x.tables().size() != y.tables().size()) return null;
    //    if (x.preds().size() != y.preds().size()) return null;

    final List<Tuple> freeVars = makeFreshVars(x.vars().size());

    for (VarAlignment alignment : alignVars(x, y, freeVars)) {
      final Conjunction xRenamed = alignment.c0();
      final Conjunction yRenamed = alignment.c1();

      final Proof proof = tdp0(xRenamed, yRenamed);
      if (proof != null) return proof;
    }

    return null;
  }

  private Proof tdp0(Conjunction x, Conjunction y) {
    final TableBijectionMatcher tableMatcher = new TableBijectionMatcher(x.tables(), y.tables());
    if (!tableMatcher.match()) return null;

    final PredMatcher predMatcher = new PredMatcher(x.preds(), y.preds());
    if (!predMatcher.match()) return null;

    final Proof negationLemma;
    final Disjunction xNeg = x.neg(), yNeg = y.neg();
    if (xNeg == null || yNeg == null) negationLemma = null;
    else {
      final Proof proof = sdp(xNeg, yNeg);
      if (proof == null) return null;
      negationLemma = helper.proveNegationEq(xNeg, yNeg, proof);
    }

    final Proof squashLemma;
    final Disjunction xSq = x.squash(), ySq = y.squash();
    if (xSq == null || ySq == null) squashLemma = null;
    else {
      final Proof proof = sdp(xSq, ySq);
      if (proof == null) return null;
      squashLemma = helper.proveSquashEq(xSq, ySq, proof);
    }

    return helper.proveConjunctionEq(x, y, negationLemma, squashLemma);
  }

  private Proof sdp(Disjunction x, Disjunction y) {
    final TermMatcher matcher = new TermMatcher(x.conjunctions(), y.conjunctions());
    if (!matcher.match()) return null;

    return helper.proveDisjunctionEqSet(x, y, matcher.proofs());
  }

  private Proof sdp(Conjunction x, Conjunction y) {
    return tdp(minimize(x), minimize(y));
  }

  private static List<Tuple> makeFreshVars(int count) {
    return FRESH_VARIABLES.subList(0, count);
  }

  private static final List<Tuple> FRESH_VARIABLES = Util.makeFreshVars(DECISION_VAR_PREFIX, 10);

  private class TermBijectionMatcher extends BijectionMatcher<Conjunction> {
    private final List<Proof> proofs;

    protected TermBijectionMatcher(List<Conjunction> xs, List<Conjunction> ys) {
      super(xs, ys);
      proofs = new ArrayList<>(xs.size());
    }

    private List<Proof> proofs() {
      return proofs;
    }

    @Override
    protected boolean tryMatch(Conjunction x, Conjunction y) {
      final Proof proof = tdp(x, y);
      if (proof == null) return false;

      proofs.add(proof);
      return true;
    }
  }

  private class TermMatcher extends Matcher<Conjunction> {
    private final List<Proof> proofs;

    protected TermMatcher(List<Conjunction> xs, List<Conjunction> ys) {
      super(xs, ys);
      this.proofs = new ArrayList<>(max(xs.size(), ys.size()));
    }

    List<Proof> proofs() {
      return proofs;
    }

    @Override
    boolean tryMatch(Conjunction x, Conjunction y) {
      final Proof proof = sdp(x, y);
      if (proof == null) return false;

      proofs.add(proof);
      return true;
    }
  }
}
