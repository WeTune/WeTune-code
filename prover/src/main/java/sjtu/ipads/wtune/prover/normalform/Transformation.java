package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.head;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.expr.UExpr.*;

public interface Transformation {
  UExpr apply(UExpr point);

  Collection<Transformation> PASS_1_TRANSFORMATIONS =
      List.of(new Associativity(), new Distribution(), new MulSum(), new SumAdd(), new SumSum());

  Collection<Transformation> PASS_2_TRANSFORMATIONS =
      List.of(new SquashCommunity(), new Associativity(), new MulSquash());

  Collection<Transformation> PASS_3_TRANSFORMATIONS =
      List.of(new NotCommunity(), new Associativity(), new MulNot());

  static UExpr transform(UExpr root) {
    root = transform(suffixTraversal(root), PASS_1_TRANSFORMATIONS);
    root = transform(suffixTraversal(root), PASS_2_TRANSFORMATIONS);
    root = transform(suffixTraversal(root), PASS_3_TRANSFORMATIONS);

    return root;
  }

  static Disjunction toNormalForm(UExpr root) {
    return toDisjunction(root);
  }

  private static Disjunction toDisjunction(UExpr root) {
    final List<UExpr> factors = factorsOf(root, Kind.ADD);
    return new Disjunction(listMap(Transformation::toConjunction, factors));
  }

  private static Conjunction toConjunction(UExpr root) {
    final boolean withSum = root.kind() == Kind.SUM;
    final UExpr expr = withSum ? root.child(0) : root;
    if (expr.kind() == Kind.SUM) throw new IllegalArgumentException("not a normal form: " + root);

    final List<UExpr> factors = factorsOf(expr, Kind.MUL);

    final List<UExpr> squash = listFilter(it -> it.kind() == Kind.SQUASH, factors);
    final List<UExpr> negation = listFilter(it -> it.kind() == Kind.NOT, factors);
    final List<UExpr> tables = listFilter(it -> it.kind() == Kind.TABLE, factors);
    final List<UExpr> predicates = Collections.emptyList(); // TODO

    if (squash.size() >= 2 || negation.size() >= 2)
      throw new IllegalArgumentException("not a normal form: " + root);

    return new Conjunction(withSum, predicates, head(negation), head(squash), tables);
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

  private static UExpr transform(List<UExpr> targets, Collection<Transformation> tfs) {
    for (UExpr target : targets)
      for (Transformation tf : tfs) {
        final UExpr applied = tf.apply(target);
        if (applied != target) return transform(suffixTraversal(rootOf(applied)), tfs);
      }

    return targets.get(targets.size() - 1);
  }

  static void main(String[] args) {
    final Tuple t = Tuple.make("t");
    final UExpr expr =
        mul(
            not(table("R", t)),
            sum(
                mul(
                    squash(table("S", t)),
                    mul(not(table("T", t)), (add(squash(table("U", t)), sum(table("V", t))))))));
    final UExpr spnf = transform(expr);
    System.out.println(expr);
    System.out.println(spnf);
    System.out.println(toNormalForm(spnf));
  }
}
