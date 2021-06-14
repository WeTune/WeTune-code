package sjtu.ipads.wtune.prover.decision;

import com.google.common.collect.Collections2;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.prover.Congruence;
import sjtu.ipads.wtune.prover.DecisionContext;
import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.*;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.Commons.toIntArray;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.EQ_PRED;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.UNINTERPRETED_PRED;
import static sjtu.ipads.wtune.prover.expr.UExpr.*;
import static sjtu.ipads.wtune.prover.normalform.Transformation.toSpnf;

public class DecisionProcedure {
  public static List<Proof> decideEq(Disjunction x1, Disjunction x2, DecisionContext ctx) {
    List<Conjunction> conj1 = x1.conjunctions(), conj2 = x2.conjunctions();
    if (conj1.size() != conj2.size()) return emptyList();

    final List<Proof> lemmas = new ArrayList<>();
    final List<Proof> coreLemmas = new ArrayList<>();
    conj2 = new ArrayList<>(conj2);

    outer:
    for (Conjunction c1 : conj1) {

      for (int i = 0; i < conj2.size(); i++) {
        final Conjunction c2 = conj2.get(i);
        final List<Proof> proofs = decideEq(c1, c2, ctx);
        if (!proofs.isEmpty()) {
          lemmas.addAll(proofs);
          coreLemmas.add(tail(lemmas));
          conj2.remove(i);
          continue outer;
        }
      }

      return emptyList();
    }

    final Proof mainTheorem = Proof.make(ctx.makeProofName());
    mainTheorem.setConclusion("%s = %s".formatted(x1, x2));

    for (Proof coreLemma : coreLemmas) mainTheorem.append("rw " + coreLemma.name());
    mainTheorem.append("ring");

    lemmas.add(mainTheorem);

    return lemmas;
  }

  private static List<Proof> decideEq(Conjunction x1, Conjunction x2, DecisionContext ctx) {
    if (x1.sumTuples().size() != x2.sumTuples().size()) return emptyList();
    if (x1.negation() == null ^ x2.negation() == null) return emptyList();
    if (x1.squash() == null ^ x2.squash() == null) return emptyList();
    if (x1.tables().size() != x2.tables().size()) return emptyList();
    if (x1.predicates().size() != x2.predicates().size()) return emptyList();

    for (List<Integer> permutation :
        Collections2.orderedPermutations(INTEGERS.get(x1.sumTuples().size()))) {
      final List<Proof> proofs = decideEq0(x1, x2, ctx, toIntArray(permutation));
      if (!proofs.isEmpty()) return proofs;
    }

    return emptyList();
  }

  private static List<Proof> decideEq0(
      Conjunction x1, Conjunction x2, DecisionContext ctx, int[] permutation) {
    final String target = "%s = %s".formatted(x1, x2);

    x1 = x1.copy();
    x2 = x2.copy();

    final List<Tuple> tuples = makeFreshVariables(permutation.length);
    final List<Tuple> sumTuples1 = x1.sumTuples(), sumTuples2 = x2.sumTuples();

    for (int i = 0; i < 1; i++) {
      x1.subst(sumTuples1.get(i), tuples.get(i));
      x2.subst(sumTuples2.get(i), tuples.get(permutation[i]));
    }

    final List<Proof> lemmasForNegation;
    if (x1.negation() != null && x2.negation() != null) {
      final var pair1 = toSpnf(x1.negation().child(0), ctx.makeProofName());
      final var pair2 = toSpnf(x2.negation().child(0), ctx.makeProofName());
      final List<Proof> lemmas = decideSquashEq(pair1.getLeft(), pair2.getLeft(), ctx);

      if (lemmas.isEmpty()) return emptyList();

      lemmasForNegation = new ArrayList<>(lemmas);
      lemmasForNegation.add(pair1.getRight());
      lemmasForNegation.add(pair2.getRight());

      final Proof theorem = Proof.make(ctx.makeProofName());
      theorem.setConclusion("%s = %s".formatted(x1.negation(), x2.negation()));
      theorem.append("rw " + pair1.getRight().name());
      theorem.append("rw " + pair2.getRight().name());
      theorem.append("apply squash_not_eq");
      theorem.append("exact " + tail(lemmas).name());
      lemmasForNegation.add(theorem);

    } else lemmasForNegation = emptyList();

    final List<Proof> lemmasForSquash;
    if (x1.squash() != null && x2.squash() != null) {
      final var pair1 = toSpnf(x1.squash().child(0), ctx.makeProofName());
      final var pair2 = toSpnf(x2.squash().child(0), ctx.makeProofName());

      final List<Proof> lemmas = decideSquashEq(pair1.getLeft(), pair2.getLeft(), ctx);
      lemmasForSquash = new ArrayList<>();
      lemmasForSquash.add(pair1.getRight());
      lemmasForSquash.add(pair2.getRight());

      final Proof theorem = Proof.make(ctx.makeProofName());
      theorem.setConclusion("%s = %s".formatted(x1.squash(), x2.squash()));
      theorem.append("rw " + pair1.getRight().name());
      theorem.append("rw " + pair2.getRight().name());
      theorem.append("exact " + tail(lemmas).name());
      lemmasForNegation.add(theorem);

    } else lemmasForSquash = emptyList();

    if (!matchTables(x1.tables(), x2.tables(), Congruence.make(), true)) return emptyList();
    if (!matchPreds(x1.predicates(), x2.predicates(), Congruence.make())) return emptyList();

    final Proof mainTheorem = Proof.make(ctx.makeProofName());
    mainTheorem.setConclusion(target);

    permuteVariables(permutation, 0, tuples, mainTheorem);

    final List<Proof> lemmas = new ArrayList<>();
    lemmas.addAll(lemmasForNegation);
    lemmas.addAll(lemmasForSquash);

    if (!lemmasForNegation.isEmpty()) mainTheorem.append("rw " + tail(lemmasForNegation).name());
    if (!lemmasForSquash.isEmpty()) mainTheorem.append("rw " + tail(lemmasForSquash).name());
    mainTheorem.append("ring");

    lemmas.add(mainTheorem);

    return lemmas;
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

  private static List<Tuple> makeFreshVariables(int count) {
    return FRESH_VARIABLES.get(count);
  }

  private static List<Proof> decideSquashEq(Disjunction x1, Disjunction x2, DecisionContext ctx) {
    final Proof mainProof = Proof.make(ctx.makeProofName());
    mainProof.setConclusion("squash (%s) = squash (%s)".formatted(x1, x2));
    mainProof.append("sorry");
    return List.of(mainProof);
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

  private static boolean matchTables(
      List<UExpr> tables1, List<UExpr> tables2, Congruence congruence, boolean oneToOne) {
    outer:
    for (UExpr expr1 : tables1) {
      final TableTerm table1 = (TableTerm) expr1;

      for (int i = 0, bound = tables2.size(); i < bound; i++) {
        final TableTerm table2 = (TableTerm) tables2.get(i);
        if (congruence.isCongruent(table1.name(), table2.name())
            && congruence.isCongruent(table1.tuple(), table2.tuple())) {

          if (oneToOne) tables2.remove(i);

          continue outer;
        }
      }

      return false;
    }

    return true;
  }

  private static boolean matchPreds(List<UExpr> preds1, List<UExpr> preds2, Congruence congruence) {
    outer:
    for (UExpr expr1 : preds1) {
      for (final UExpr expr2 : preds2) {
        if (expr1.kind() != expr2.kind()) continue;

        if (expr1.kind() == UNINTERPRETED_PRED) {
          final UninterpretedPredTerm pred1 = (UninterpretedPredTerm) expr1;
          final UninterpretedPredTerm pred2 = (UninterpretedPredTerm) expr2;
          if (!congruence.isCongruent(pred1.name(), pred2.name())
              || !congruence.isCongruent(pred1.tuple(), pred2.tuple())) continue;

        } else if (expr1.kind() == EQ_PRED) {
          final EqPredTerm pred1 = (EqPredTerm) expr1;
          final EqPredTerm pred2 = (EqPredTerm) expr2;
          if (!congruence.isCongruent(pred1.left(), pred2.left())) continue;

        } else assert false;

        continue outer;
      }

      return false;
    }

    return true;
  }

  private static boolean permuteDone(int[] permutation) {
    for (int i : permutation) if (permutation[i] != i) return false;
    return true;
  }

  public static void main(String[] args) {
    final Tuple t = Tuple.make("t");
    final Tuple t1 = Tuple.make("t1");
    final Tuple t2 = Tuple.make("t2");
    final UExpr expr0 =
        mul(
            uninterpretedPred("p1", t),
            sum(t1, mul(eqPred(t1, t.proj("x")), mul(table("T", t1), table("S", t1)))));
    final UExpr expr1 =
        sum(
            t2,
            mul(
                uninterpretedPred("p1", t),
                mul(eqPred(t2, t.proj("x")), mul(table("S", t2), table("T", t2)))));

    final DecisionContext ctx = DecisionContext.make();
    final Pair<Disjunction, Proof> u0 = toSpnf(expr0, ctx.makeProofName());
    final Pair<Disjunction, Proof> u1 = toSpnf(expr1, ctx.makeProofName());
    System.out.println(u0.getRight().stringify());
    System.out.println(u1.getRight().stringify());
    final List<Proof> proofs = decideEq(u0.getLeft(), u1.getLeft(), ctx);
    for (Proof proof : proofs) {
      System.out.println(proof.stringify());
    }
    final Proof proof = Proof.make("query_eq");
    proof.setConclusion("%s = %s".formatted(expr0, expr1));
    proof.append("rw " + u0.getRight().name());
    proof.append("rw " + u1.getRight().name());
    proof.append("rw " + tail(proofs).name());

    System.out.println(proof.stringify());
  }
}
