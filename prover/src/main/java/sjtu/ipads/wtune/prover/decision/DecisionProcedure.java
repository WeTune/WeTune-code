package sjtu.ipads.wtune.prover.decision;

import static com.google.common.collect.Collections2.permutations;
import static java.lang.Math.max;
import static sjtu.ipads.wtune.common.utils.Commons.toIntArray;
import static sjtu.ipads.wtune.prover.ProverSupport.normalize;
import static sjtu.ipads.wtune.prover.ProverSupport.translateExpr;
import static sjtu.ipads.wtune.prover.decision.Minimization.minimize;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.EQ_PRED;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.PRED;
import static sjtu.ipads.wtune.prover.utils.Util.arrange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import sjtu.ipads.wtune.prover.DecisionContext;
import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.EqPredTerm;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UninterpretedPredTerm;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.utils.Congruence;
import sjtu.ipads.wtune.prover.utils.TupleCongruence;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;

public class DecisionProcedure {
  private final DecisionContext ctx;
  private final Disjunction x, y;
  private final ProofHelper helper;

  DecisionProcedure(DecisionContext ctx, Disjunction x, Disjunction y) {
    this.ctx = ctx;
    this.x = x;
    this.y = y;
    this.helper = new ProofHelper(ctx);
  }

  public static Proof decide(DecisionContext ctx, Disjunction x, Disjunction y) {
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
    if (x.boundedVars().size() != y.boundedVars().size()) return null;
    if (x.negation() == null ^ y.negation() == null) return null;
    if (x.squash() == null ^ y.squash() == null) return null;
    if (x.tables().size() != y.tables().size()) return null;
    if (x.predicates().size() != y.predicates().size()) return null;

    final int numVars = x.boundedVars().size();
    for (List<Integer> permutation : permutations(INTEGERS.get(numVars))) {
      final List<Tuple> freeVars = makeFreshVars(numVars);
      final Conjunction xRenamed = substBoundedVars(x, freeVars);
      final Conjunction yRenamed = substBoundedVars(y, arrange(freeVars, toIntArray(permutation)));

      final Proof proof = tdp0(xRenamed, yRenamed);
      if (proof != null) return proof;
    }

    return null;
  }

  private Proof tdp0(Conjunction x, Conjunction y) {
    final TableBijectionMatcher tableMatcher = new TableBijectionMatcher(x.tables(), y.tables());
    if (!tableMatcher.match()) return null;

    final PredMatcher predMatcher = new PredMatcher(x.predicates(), y.predicates());
    if (!predMatcher.match()) return null;

    final Proof negationLemma;
    final Disjunction xNeg = x.negation(), yNeg = y.negation();
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
    return FRESH_VARIABLES.get(count);
  }

  private static Conjunction substBoundedVars(Conjunction x, List<Tuple> tuples) {
    final Conjunction copy = x.copy();
    final List<Tuple> vars = copy.boundedVars();
    for (int i = 0, bound = tuples.size(); i < bound; i++) {
      copy.subst(vars.get(i), tuples.get(i));
    }
    return copy;
  }

  private static final List<List<Integer>> INTEGERS;
  private static final List<List<Tuple>> FRESH_VARIABLES;

  static {
    INTEGERS =
        IntStream.range(0, 10)
            .mapToObj(count -> IntStream.range(0, count).boxed().toList())
            .toList();
    FRESH_VARIABLES =
        IntStream.range(0, 10)
            .mapToObj(
                count -> IntStream.range(0, count).mapToObj(i -> Tuple.make("x" + i)).toList())
            .toList();
  }

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
    boolean tryMatch(Conjunction x, Conjunction y) {
      final Proof proof = tdp(x, y);
      if (proof == null) return false;

      proofs.add(proof);
      return true;
    }

    @Override
    boolean onMatched(List<Conjunction> xs, List<Conjunction> ys) {
      return true;
    }
  }

  private static class TableBijectionMatcher extends BijectionMatcher<UExpr> {
    protected TableBijectionMatcher(List<UExpr> xs, List<UExpr> ys) {
      super(xs, ys);
    }

    @Override
    boolean tryMatch(UExpr x, UExpr y) {
      final TableTerm tx = (TableTerm) x, ty = (TableTerm) y;
      return tx.name().equals(ty.name()) && tx.tuple().equals(ty.tuple());
    }

    @Override
    boolean onMatched(List<UExpr> xs, List<UExpr> ys) {
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

  private static class PredMatcher extends Matcher<UExpr> {
    private final Congruence<Tuple> xCongruence, yCongruence;

    protected PredMatcher(List<UExpr> xs, List<UExpr> ys) {
      super(xs, ys);
      xCongruence = TupleCongruence.make(xs);
      yCongruence = TupleCongruence.make(ys);
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
          if (xArgs.length != yArgs.length) return false;

          for (int i = 0, bound = xArgs.length; i < bound; ++i)
            if (!xCongruence.isCongruent(xArgs[i], yArgs[i])
                || !yCongruence.isCongruent(xArgs[i], yArgs[i])) return false;

          return true;

        } else {
          return false;
        }
      }

      return false;
    }
  }

  private static void test0() {
    final Statement stmt0 = Statement.make("test", "SELECT DISTINCT d.p FROM d INNER JOIN c", null);
    final Statement stmt1 = Statement.make("test", "SELECT DISTINCT d.p FROM d", null);
    final Schema schema = stmt0.app().schema("base");

    final PlanNode plan0 = PlanSupport.assemblePlan(stmt0.parsed(), schema);
    final PlanNode plan1 = PlanSupport.assemblePlan(stmt1.parsed(), schema);

    final UExpr e0 = translateExpr(plan0);
    final UExpr e1 = translateExpr(plan1);

    final DecisionContext ctx = DecisionContext.make(e0, e1);
    ctx.setSchema(schema);

    final Disjunction d0 = normalize(e0, ctx);
    final Disjunction d1 = normalize(e1, ctx);

    System.out.println(d0);
    System.out.println(d1);

    final Proof proof = decide(ctx, d0, d1);
    System.out.println(proof != null);
  }

  public static void main(String[] args) {
    test0();
  }
}
