package sjtu.ipads.wtune.prover.decision;

import static sjtu.ipads.wtune.prover.decision.BijectionMatcher.makeBijectionMatcher;
import static sjtu.ipads.wtune.prover.utils.PermutationIter.permute;
import static sjtu.ipads.wtune.prover.utils.Util.arrange;
import static sjtu.ipads.wtune.prover.utils.Util.substBoundedVars;

import java.util.List;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.utils.Util;

// Determine if two expression is literal same up to 1. renaming and 2. equivalence closure.
public class SimpleDecisionProcedure {
  private int nextVarIdx = 0;

  public static boolean decideSame(Disjunction d0, Disjunction d1) {
    return makeBijectionMatcher(
            d0.conjunctions(), d1.conjunctions(), SimpleDecisionProcedure::decideSame)
        .match();
  }

  public static boolean decideSame(Conjunction c0, Conjunction c1) {
    return new SimpleDecisionProcedure().decideSame0(c0, c1) != null;
  }

  /**
   * Check if two conjunction the same up to renaming.
   *
   * <p>e.g. Sum{t1,t2}(f(t1,t2)) is the same as Sum{x1,x2}(f(x2,x1)) by the renaming
   * {x1=>t2,x2=>t1}.
   *
   * @return a variable remapping where two conjunctions are the same. otherwise null.
   */
  private int[] decideSame0(Conjunction c0, Conjunction c1) {
    if (c0.vars().size() != c1.vars().size()) return null;

    final int numVars = c0.vars().size();
    final List<Tuple> tempVars = makeTempVars(numVars);
    final Conjunction copy0 = substBoundedVars(c0, tempVars);

    for (int[] permutation : permute(numVars, numVars)) {
      final Conjunction copy1 = substBoundedVars(c1, arrange(tempVars, permutation));

      if (compareTables(copy0.tables(), copy1.tables())
          && comparePreds(copy0.predicates(), copy1.predicates())
          && compareNeg(copy0.negation(), copy1.negation())
          && compareSquash(copy0.squash(), copy1.squash())) return permutation;
    }

    return null;
  }

  private boolean compareTables(List<UExpr> tables0, List<UExpr> tables1) {
    return new TableBijectionMatcher(tables0, tables1).match();
  }

  private boolean comparePreds(List<UExpr> preds0, List<UExpr> preds1) {
    return new PredMatcher(preds0, preds1).match();
  }

  private boolean compareNeg(Disjunction d0, Disjunction d1) {
    if (d0 == null ^ d1 == null) return false;
    if (d0 == null) return true;

    return compareDisjunction(d0, d1);
  }

  private boolean compareSquash(Disjunction d0, Disjunction d1) {
    if (d0 == null ^ d1 == null) return false;
    if (d0 == null) return true;

    return compareDisjunction(d0, d1);
  }

  private boolean compareDisjunction(Disjunction d0, Disjunction d1) {
    return false;
    //    return makeBijectionMatcher(
    //            d0.conjunctions(), d1.conjunctions(), (x, y) -> decideSame(x, y) != null)
    //        .match();
  }

  private List<Tuple> makeTempVars(int count) {
    final List<Tuple> vars = Util.makeTempVars0(nextVarIdx, count);
    nextVarIdx += count;
    return vars;
  }
}
