package sjtu.ipads.wtune.prover.normalform;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.SumExpr;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.head;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.expr.UExpr.*;

public interface Transformation {
  UExpr apply(UExpr point);

  void setProof(Proof ctx);

  static Pair<Disjunction, Proof> toSpnf(UExpr root, String proofName) {
    final Pair<UExpr, Proof> pair = transform(root, proofName);
    return Pair.of(wrapAsSpnf(pair.getKey()), pair.getRight());
  }

  private static Pair<UExpr, Proof> transform(UExpr root, String proofName) {
    final String original = root.toString();

    final Proof proof = Proof.make(proofName);
    root = transform(suffixTraversal(root), transformationPass1(proof));
    root = transform(suffixTraversal(root), transformationPass2(proof));
    root = transform(suffixTraversal(root), transformationPass3(proof));

    proof.setConclusion("%s = %s".formatted(original, root));

    return Pair.of(root, proof);
  }

  private static UExpr transform(List<UExpr> targets, Collection<Transformation> tfs) {
    for (UExpr target : targets)
      for (Transformation tf : tfs) {
        final UExpr applied = tf.apply(target);
        validate(applied);
        if (applied != target) return transform(suffixTraversal(rootOf(applied)), tfs);
      }

    return targets.get(targets.size() - 1);
  }

  // select * from T where exists (select * from S where T.i = S.j)
  // T(t) * || Sum{t'}(S(t') * [t'.i = t.j]) ||
  private static Collection<Transformation> transformationPass1(Proof proof) {
    final List<Transformation> tfs =
        List.of(new Associativity(), new Distribution(), new MulSum(), new SumAdd(), new SumSum());
    for (Transformation tf : tfs) tf.setProof(proof);
    return tfs;
  }

  private static Collection<Transformation> transformationPass2(Proof proof) {
    final List<Transformation> tfs =
        List.of(new SquashCommunity(), new Associativity(), new MulSquash());
    for (Transformation tf : tfs) tf.setProof(proof);
    return tfs;
  }

  private static Collection<Transformation> transformationPass3(Proof proof) {
    final List<Transformation> tfs = List.of(new NotCommunity(), new Associativity(), new MulNot());
    for (Transformation tf : tfs) tf.setProof(proof);
    return tfs;
  }

  private static Disjunction wrapAsSpnf(UExpr root) {
    return toDisjunction(root);
  }

  private static Disjunction toDisjunction(UExpr root) {
    final List<UExpr> factors = factorsOf(root, Kind.ADD);
    return new DisjunctionImpl(listMap(Transformation::toConjunction, factors));
  }

  private static Conjunction toConjunction(UExpr root) {
    final boolean withSum = root.kind() == Kind.SUM;
    final UExpr expr = withSum ? root.child(0) : root;
    if (expr.kind() == Kind.SUM) throw new IllegalArgumentException("not a normal form: " + root);

    final List<UExpr> factors = factorsOf(expr, Kind.MUL);

    final List<Tuple> boundTuples = withSum ? ((SumExpr) root).boundTuples() : emptyList();
    final List<UExpr> squashes = listFilter(it -> it.kind() == Kind.SQUASH, factors);
    final List<UExpr> negations = listFilter(it -> it.kind() == Kind.NOT, factors);
    final List<UExpr> tables = listFilter(it -> it.kind() == Kind.TABLE, factors);
    final List<UExpr> predicates = listFilter(it -> it.kind().isPred(), factors);

    if (squashes.size() >= 2 || negations.size() >= 2)
      throw new IllegalArgumentException("not a normal form: " + root);

    return new ConjunctionImpl(boundTuples, tables, predicates, head(squashes), head(negations));
  }

  private static List<UExpr> factorsOf(UExpr root, Kind connection) {
    if (connection.numChildren != 2) throw new IllegalArgumentException();

    final List<UExpr> factors = new ArrayList<>();
    UExpr expr = root;
    while (expr.kind() == connection) {
      final UExpr factor = expr.child(1);
      if (factor.kind() == connection)
        throw new IllegalArgumentException("not a normal form: " + root);
      factors.add(factor);
      expr = expr.child(0);
    }
    factors.add(expr);
    return factors;
  }

  static void main(String[] args) {
    final Tuple t = Tuple.make("t");
    final Tuple x1 = Tuple.make("x1");
    final Tuple x2 = Tuple.make("x2");
    final Tuple x3 = Tuple.make("x3");
    final UExpr expr =
        sum(
            x1,
            mul(
                eqPred(t, x1),
                mul(
                    not(table("R", x1)),
                    sum(
                        x2,
                        mul(
                            squash(table("S", x2)),
                            mul(
                                not(table("T", x2)),
                                (add(squash(table("U", t)), sum(x3, table("V", x3))))))))));
    System.out.println(expr);
    final var result = transform(expr, "spnf");
    final UExpr spnf = result.getLeft();
    final Proof proof = result.getRight();
    System.out.println(spnf);
    System.out.println(wrapAsSpnf(spnf));
    System.out.println(proof.stringify());
  }
}
