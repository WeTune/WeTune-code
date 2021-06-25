package sjtu.ipads.wtune.prover.decision;

import java.util.List;
import sjtu.ipads.wtune.prover.DecisionContext;
import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;

final class ProofHelper {
  private final DecisionContext ctx;

  ProofHelper(DecisionContext ctx) {
    this.ctx = ctx;
  }

  Proof proveDisjunctionEqBag(Disjunction x, Disjunction y, List<Proof> factorEqLemmas) {
    final Proof theorem = ctx.newProof().setConclusion("%s = %s".formatted(x, y));
    for (Proof proof : factorEqLemmas) theorem.append("rw " + proof.name());
    theorem.append("ring");
    return theorem;
  }

  Proof proveDisjunctionEqSet(Disjunction x, Disjunction y, List<Proof> factorEqLemmas) {
    // TODO
    final Proof theorem = ctx.newProof().setConclusion("%s = %s".formatted(x, y));
    for (Proof proof : factorEqLemmas) theorem.append("rw " + proof.name());
    theorem.append("ring");
    return theorem;
  }

  Proof proveConjunctionEq(
      Conjunction x, Conjunction y, Proof negationEqLemma, Proof squashEqLemma) {
    final Proof theorem = ctx.newProof().setConclusion("%s = %s".formatted(x, y));
    if (negationEqLemma != null) theorem.append("rw " + negationEqLemma.name());
    if (squashEqLemma != null) theorem.append("rw " + squashEqLemma.name());
    theorem.append("ring");
    return theorem;
  }

  Proof proveNegationEq(Disjunction x, Disjunction y, Proof eqLemma) {
    return ctx.newProof()
        .setConclusion("not(%s) = not(%s)".formatted(x, y))
        .append("apply squash_not_eq")
        .append("exact " + eqLemma.name());
  }

  Proof proveSquashEq(Disjunction x, Disjunction y, Proof eqLemma) {
    return ctx.newProof()
        .setConclusion("squash(%s) = squash(%s)".formatted(x, y))
        .append("exact " + eqLemma);
  }

  private static void permuteVariables(
      int[] permutation, int base, List<Tuple> tuples, Proof proof) {
    if (base >= permutation.length) return;
    if (permutation[base] != base) {
      int pos = base + 1, bound = permutation.length;
      for (; pos < bound; pos++) if (permutation[pos] == base) break;
      assert pos < bound;
      proof.append("conv {to_rhs,rw sum_comm%d}".formatted(pos - base + 1));
      permutation[pos] = permutation[base];
      permutation[base] = base;
    }
    //    if (permuteDone(permutation)) return;

    proof.append("apply sum_eq");
    proof.append("funext " + tuples.get(base).name());
    permuteVariables(permutation, base + 1, tuples, proof);
  }

  private static boolean permuteDone(int[] permutation) {
    for (int i : permutation) if (permutation[i] != i) return false;
    return true;
  }
}
