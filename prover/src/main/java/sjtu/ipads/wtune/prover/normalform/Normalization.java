package sjtu.ipads.wtune.prover.normalform;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.ProverSupport.translateExpr;
import static sjtu.ipads.wtune.prover.expr.UExpr.rootOf;
import static sjtu.ipads.wtune.prover.expr.UExpr.suffixTraversal;
import static sjtu.ipads.wtune.prover.normalform.Canonization.applyForeignKey;
import static sjtu.ipads.wtune.prover.normalform.Canonization.applyUniqueKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import sjtu.ipads.wtune.prover.DecisionContext;
import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.SumExpr;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.stmt.Statement;

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
      final UExpr expr = applyForeignKey(copy, ctx.foreignKeys());
      final Proof proof = ctx.newProof();
      final Disjunction normalForm = asDisjunction(new Normalization(proof).transform(expr));
      final Disjunction canonicalForm = applyUniqueKey(normalForm, ctx.uniqueKeys());
      proof.setConclusion("%s = %s".formatted(root, canonicalForm));
      return canonicalForm;

    } else {
      final Proof proof = Proof.makeNull();
      final Disjunction normalForm = asDisjunction(new Normalization(proof).transform(copy));
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

  private static Disjunction asDisjunction(UExpr root) {
    final List<UExpr> factors = listFactors(root, Kind.ADD);
    return new DisjunctionImpl(listMap(Normalization::asConjunction, factors));
  }

  private static Conjunction asConjunction(UExpr root) {
    final boolean withSum = root.kind() == Kind.SUM;
    final UExpr expr = withSum ? root.child(0) : root;
    if (expr.kind() == Kind.SUM) throw new IllegalArgumentException("not a normal form: " + root);

    final List<UExpr> factors = listFactors(expr, Kind.MUL);

    final List<Tuple> boundedVars = withSum ? ((SumExpr) root).boundedVars() : emptyList();
    final List<UExpr> squashes = listFilter(it -> it.kind() == Kind.SQUASH, factors);
    final List<UExpr> negations = listFilter(it -> it.kind() == Kind.NOT, factors);
    final List<UExpr> tables = listFilter(it -> it.kind() == Kind.TABLE, factors);
    final List<UExpr> predicates = listFilter(it -> it.kind().isPred(), factors);

    if (squashes.size() >= 2 || negations.size() >= 2)
      throw new IllegalArgumentException("not a normal form: " + root);

    return new ConjunctionImpl(
        boundedVars,
        tables,
        predicates,
        squashes.isEmpty() ? null : asDisjunction(squashes.get(0).child(0)),
        negations.isEmpty() ? null : asDisjunction(negations.get(0).child(0)));
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

  private static void test0() {
    final Tuple t0 = Tuple.make("t0");
    final Tuple t1 = Tuple.make("t1");
    final Tuple t2 = Tuple.make("t2");

    final UExpr expr =
        UExpr.squash(
            UExpr.mul(
                UExpr.table("X", t0),
                UExpr.squash(UExpr.add(UExpr.table("Y", t1), UExpr.table("Z", t2)))));
    System.out.println(normalize(expr, null));
  }

  private static void test1() {
    final Statement stmt0 =
        Statement.make("test", "SELECT d0.p FROM d AS d0 JOIN c AS c1 WHERE d0.q = c1.v", null);
    final Collection<Constraint> constraints =
        stmt0.app().schema("base").table("d").column("q").constraints(ConstraintType.FOREIGN);

    final UExpr originalExpr =
        translateExpr(PlanSupport.assemblePlan(stmt0.parsed(), stmt0.app().schema("base")));
    System.out.println(originalExpr);
    System.out.println(applyForeignKey(originalExpr, constraints));
    final Disjunction expr0 = normalize(originalExpr, null);
    System.out.println(expr0);
    //    System.out.println(applyUniqueKey(expr0, constraints));
  }

  public static void main(String[] args) {}
}
