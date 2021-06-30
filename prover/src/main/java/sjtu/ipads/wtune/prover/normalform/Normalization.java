package sjtu.ipads.wtune.prover.normalform;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.Commons.head;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.expr.UExpr.preorderTraversal;
import static sjtu.ipads.wtune.prover.expr.UExpr.rootOf;
import static sjtu.ipads.wtune.prover.utils.Constants.NORMALIZATION_VAR_PREFIX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.SumExpr;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;
import sjtu.ipads.wtune.prover.utils.Counter;

public class Normalization {
  private static final List<Transformation> PASS0 =
      List.of(
          new ElimSquash(),
          new SumMul(),
          new SumAdd(),
          new NotAdd(),
          new NotMul(),
          new NotNot(),
          new SquashNot(),
          new SquashSquash(),
          new Associativity(),
          new Distribution());
  // Section 3.3, Eq 1-9
  private static final List<Transformation> PASS1 = List.of(new MulSum(), new SumSum());
  private static final List<Transformation> PASS2 =
      List.of(new SquashCommunity(), new Associativity(), new MulSquash());
  private static final List<Transformation> PASS3 =
      List.of(new NotCommunity(), new Associativity(), new MulNot());

  private final Proof proof;

  Normalization(Proof proof) {
    this.proof = coalesce(proof, Proof.makeNull());
  }

  public static Disjunction normalize(UExpr root) {
    return asDisjunction(new Normalization(null).transform(root.copy()), new Counter());
  }

  private UExpr transform(UExpr root) {
    root = transform(UExpr.postorderTraversal(root), PASS0);
    root = transform(UExpr.postorderTraversal(root), PASS1);
    root = transform(UExpr.postorderTraversal(root), PASS2);
    root = transform(UExpr.postorderTraversal(root), PASS3);
    return root;
  }

  private UExpr transform(List<UExpr> targets, Collection<Transformation> tfs) {
    // not efficient, but safe
    for (UExpr target : targets)
      for (Transformation tf : tfs) {
        final UExpr applied = tf.apply(target, proof);
        if (applied != target) {
          return transform(UExpr.postorderTraversal(rootOf(applied)), tfs);
        }
      }

    return head(targets);
  }

  private static Disjunction asDisjunction(UExpr root, Counter counter) {
    final List<UExpr> factors = listFactors(root, Kind.ADD);
    return new DisjunctionImpl(listMap(it -> asConjunction(it, counter), factors));
  }

  private static Conjunction asConjunction(UExpr root, Counter counter) {
    final UExpr expr;
    final List<Tuple> originalBoundedVars;
    if (root.kind() == Kind.SUM) {
      expr = root.child(0);
      originalBoundedVars = ((SumExpr) root).boundedVars();
    } else {
      expr = root;
      originalBoundedVars = emptyList();
    }

    if (expr.kind() == Kind.SUM) throw new IllegalArgumentException("not a normal form: " + root);

    final List<Tuple> boundedVars = splitVariables(expr, originalBoundedVars, counter);
    final List<UExpr> factors = listFactors(expr, Kind.MUL);
    final List<UExpr> tables = listFilter(it -> it.kind() == Kind.TABLE, factors);
    final List<UExpr> squashes = listFilter(it -> it.kind() == Kind.SQUASH, factors);
    final List<UExpr> negations = listFilter(it -> it.kind() == Kind.NOT, factors);
    final List<UExpr> predicates = listFilter(it -> it.kind().isPred(), factors);

    if (squashes.size() >= 2 || negations.size() >= 2)
      throw new IllegalArgumentException("not a normal form: " + root);

    return Conjunction.make(
        boundedVars,
        tables,
        predicates,
        squashes.isEmpty() ? null : asDisjunction(squashes.get(0).child(0), counter),
        negations.isEmpty() ? null : asDisjunction(negations.get(0).child(0), counter));
  }

  private static List<UExpr> listFactors(UExpr root, Kind connection) {
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

  private static List<Tuple> splitVariables(UExpr expr, List<Tuple> originalVars, Counter counter) {
    final List<UExpr> tables = preorderTraversal(expr, Kind.TABLE);
    final List<Tuple> variables = new ArrayList<>(tables.size());
    final Set<Tuple> split = new HashSet<>(originalVars.size());

    for (UExpr e : tables) {
      final TableTerm tableTerm = (TableTerm) e;
      final Tuple tuple = tableTerm.tuple();
      if (tuple.isBase() || !originalVars.contains(tuple.base()[0])) continue;

      final Tuple variable = Tuple.make(NORMALIZATION_VAR_PREFIX + counter.addAssign());
      expr.subst(tuple, variable);
      variables.add(variable);
      split.add(tuple.base()[0]);
    }

    if (split.size() != originalVars.size())
      for (Tuple originalVar : originalVars)
        if (!split.contains(originalVar)) variables.add(originalVar);

    return variables;
  }
}
