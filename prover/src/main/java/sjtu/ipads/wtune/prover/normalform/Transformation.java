package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.DecisionContext;
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

  void setContext(DecisionContext ctx);

  Collection<Transformation> PASS_3_TRANSFORMATIONS =
      List.of(new NotCommunity(), new Associativity(), new MulNot());

  static UExpr transform(UExpr root, DecisionContext ctx) {
    ctx.openTracer("spnf");

    final String original = root.toString();

    root = transform(suffixTraversal(root), transformationPass1(ctx));
    root = transform(suffixTraversal(root), transformationPass2(ctx));
    root = transform(suffixTraversal(root), transformationPass3(ctx));

    ctx.currentTracer().setPrologue("lemma spnf : %s = %s := begin\n".formatted(original, root));
    ctx.currentTracer().setEpilogue("\nend");

    return root;
  }

  static Disjunction toNormalForm(UExpr root) {
    return toDisjunction(root);
  }

  private static Collection<Transformation> transformationPass1(DecisionContext ctx) {
    final List<Transformation> tfs =
        List.of(new Associativity(), new Distribution(), new MulSum(), new SumAdd(), new SumSum());
    for (Transformation tf : tfs) tf.setContext(ctx);
    return tfs;
  }

  private static Collection<Transformation> transformationPass2(DecisionContext ctx) {
    final List<Transformation> tfs =
        List.of(new SquashCommunity(), new Associativity(), new MulSquash());
    for (Transformation tf : tfs) tf.setContext(ctx);
    return tfs;
  }

  private static Collection<Transformation> transformationPass3(DecisionContext ctx) {
    final List<Transformation> tfs = List.of(new NotCommunity(), new Associativity(), new MulNot());
    for (Transformation tf : tfs) tf.setContext(ctx);
    return tfs;
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
    final DecisionContext ctx = DecisionContext.make();
    final Tuple t = Tuple.make("t");
    final UExpr expr =
        mul(
            not(table("R", t)),
            sum(
                mul(
                    squash(table("S", t)),
                    mul(not(table("T", t)), (add(squash(table("U", t)), sum(table("V", t))))))));
    final UExpr spnf = transform(expr, ctx);
    System.out.println(expr);
    System.out.println(spnf);
    System.out.println(toNormalForm(spnf));
    System.out.println(ctx.currentTracer().getTrace());
  }
}
