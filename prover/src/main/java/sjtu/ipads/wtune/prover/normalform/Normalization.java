package sjtu.ipads.wtune.prover.normalform;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.head;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.ADD;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.MUL;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.NOT;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.SQUASH;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.TABLE;
import static sjtu.ipads.wtune.prover.expr.UExpr.rootOf;
import static sjtu.ipads.wtune.prover.utils.Constants.NORMALIZATION_VAR_PREFIX;
import static sjtu.ipads.wtune.prover.utils.Util.renameVars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import sjtu.ipads.wtune.prover.expr.SumExpr;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;

public class Normalization {
  // Section 3.3, Eq 1-9
  private static final List<Transformation> PASS0 =
      List.of(
          new ElimSquash(),
          new SumMul(),
          new SumAdd(),
          new NotAdd(),
          new NotMul(),
          new NotNot(),
          new SquashNot(),
          new MulAssociativity(),
          new AddAssociativity(),
          new Distribution());
  private static final List<Transformation> PASS1 = List.of(new MulSum(), new SumSum());
  private static final List<Transformation> PASS2 =
      List.of(
          new SquashCommunity(), new MulAssociativity(), new AddAssociativity(), new MulSquash());
  private static final List<Transformation> PASS3 =
      List.of(new NotCommunity(), new MulAssociativity(), new AddAssociativity(), new MulNot());

  Normalization() {}

  public static Disjunction normalize(UExpr root) {
    return normalize(root, NORMALIZATION_VAR_PREFIX);
  }

  public static Disjunction normalize(UExpr root, String renamePrefix) {
    final Disjunction d = asDisjunction(new Normalization().transform(root.copy()));
    return renamePrefix == null ? d : renameVars(d, renamePrefix);
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
        final UExpr applied = tf.apply(target);
        if (applied != target) {
          return transform(UExpr.postorderTraversal(rootOf(applied)), tfs);
        }
      }

    return head(targets);
  }

  private static Disjunction asDisjunction(UExpr root) {
    final List<UExpr> factors = gatherFactors(root, ADD);
    return Disjunction.mk(listMap(Normalization::asConjunction, factors));
  }

  private static Conjunction asConjunction(UExpr root) {
    final UExpr expr;
    final List<Tuple> vars;
    if (root.kind() == Kind.SUM) {
      expr = root.child(0);
      vars = ((SumExpr) root).boundedVars();
    } else {
      expr = root;
      vars = emptyList();
    }

    if (expr.kind() == Kind.SUM) throw new IllegalArgumentException("not a normal form: " + root);

    final List<UExpr> factors = gatherFactors(expr, MUL);
    final List<UExpr> tables = listFilter(factors, it -> it.kind() == TABLE);
    final List<UExpr> sqs = listFilter(factors, it -> it.kind() == SQUASH);
    final List<UExpr> negs = listFilter(factors, it -> it.kind() == NOT);
    final List<UExpr> predicates = listFilter(factors, it -> it.kind().isPred());

    if (sqs.size() >= 2 || negs.size() >= 2)
      throw new IllegalArgumentException("not a normal form: " + root);

    return Conjunction.mk(
        vars,
        tables,
        predicates,
        sqs.isEmpty() ? null : asDisjunction(sqs.get(0).child(0)),
        negs.isEmpty() ? null : asDisjunction(negs.get(0).child(0)));
  }

  private static List<UExpr> gatherFactors(UExpr root, Kind connection) {
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
}
