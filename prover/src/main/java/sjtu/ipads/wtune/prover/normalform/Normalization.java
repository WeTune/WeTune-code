package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.expr.UExpr.rootOf;
import static sjtu.ipads.wtune.prover.expr.UExpr.suffixTraversal;
import static sjtu.ipads.wtune.prover.normalform.Canonization.applyForeignKey;
import static sjtu.ipads.wtune.prover.normalform.Canonization.applyUniqueKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import sjtu.ipads.wtune.prover.DecisionContext;
import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;

public class Normalization {
  // Section 3.3, Eq 1-9
  private static final List<Transformation> PASS1 =
      List.of(
          new Associativity(),
          new Distribution(),
          new MulSum(),
          new SumAdd(),
          new SumSum(),
          new ElimSquash());
  private static final List<Transformation> PASS2 =
      List.of(new SquashCommunity(), new Associativity(), new MulSquash());
  private static final List<Transformation> PASS3 =
      List.of(new NotCommunity(), new Associativity(), new MulNot());

  private final Proof proof;

  Normalization(Proof proof) {
    this.proof = coalesce(proof, Proof.makeNull());
  }

  public static Disjunction normalize(UExpr root, DecisionContext ctx) {
    final UExpr copy = root.copy();
    if (ctx != null) {
      final Disjunction normalForm = asDisjunction(new Normalization(null).transform(copy), "a");
      return applyForeignKey(applyUniqueKey(normalForm, ctx.uniqueKeys()), ctx.foreignKeys());

    } else {
      final Proof proof = Proof.makeNull();
      final Disjunction normalForm = asDisjunction(new Normalization(proof).transform(copy), "a");
      proof.setConclusion("%s = %s".formatted(root, normalForm));
      return normalForm;
    }
  }

  private UExpr transform(UExpr root) {
    root = transform(suffixTraversal(root), PASS1);
    root = transform(suffixTraversal(root), PASS2);
    root = transform(suffixTraversal(root), PASS3);
    return root;
  }

  private UExpr transform(List<UExpr> targets, Collection<Transformation> tfs) {
    for (UExpr target : targets)
      for (Transformation tf : tfs) {
        final UExpr applied = tf.apply(target, proof);
        if (applied != target) return transform(suffixTraversal(rootOf(applied)), tfs);
      }

    return tail(targets);
  }

  private static Disjunction asDisjunction(UExpr root, String varPrefix) {
    final List<UExpr> factors = listFactors(root, Kind.ADD);
    return new DisjunctionImpl(listMap(it -> asConjunction(it, varPrefix), factors));
  }

  private static Conjunction asConjunction(UExpr root, String varPrefix) {
    final boolean withSum = root.kind() == Kind.SUM;
    final UExpr expr = withSum ? root.child(0) : root;
    if (expr.kind() == Kind.SUM) throw new IllegalArgumentException("not a normal form: " + root);

    final List<Tuple> boundedVars = splitVariables(expr, varPrefix);
    final List<UExpr> factors = listFactors(expr, Kind.MUL);
    final List<UExpr> tables = listFilter(it -> it.kind() == Kind.TABLE, factors);
    final List<UExpr> squashes = listFilter(it -> it.kind() == Kind.SQUASH, factors);
    final List<UExpr> negations = listFilter(it -> it.kind() == Kind.NOT, factors);
    final List<UExpr> predicates = listFilter(it -> it.kind().isPred(), factors);

    if (squashes.size() >= 2 || negations.size() >= 2)
      throw new IllegalArgumentException("not a normal form: " + root);

    return new ConjunctionImpl(
        boundedVars,
        tables,
        predicates,
        squashes.isEmpty() ? null : asDisjunction(squashes.get(0).child(0), varPrefix + "s"),
        negations.isEmpty() ? null : asDisjunction(negations.get(0).child(0), varPrefix + "n"));
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

  private static List<Tuple> splitVariables(UExpr expr, String varPrefix) {
    final List<UExpr> tables =
        listFilter(it -> it.kind() == Kind.TABLE, listFactors(expr, Kind.MUL));

    final List<Tuple> variables = new ArrayList<>(tables.size());
    int idx = 0;

    for (UExpr table : tables) {
      final Tuple variable = Tuple.make(varPrefix + (idx++));
      expr.subst(((TableTerm) table).tuple(), variable);
      variables.add(variable);
    }

    return variables;
  }
}
