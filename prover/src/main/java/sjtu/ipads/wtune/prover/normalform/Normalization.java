package sjtu.ipads.wtune.prover.normalform;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.Commons.head;
import static sjtu.ipads.wtune.common.utils.FuncUtils.all;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.prover.expr.UExpr.rootOf;
import static sjtu.ipads.wtune.prover.utils.Constants.NORMALIZATION_VAR_PREFIX;
import static sjtu.ipads.wtune.prover.utils.Constants.TEMP_VAR_PREFIX_0;
import static sjtu.ipads.wtune.prover.utils.Util.renameVars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.EqPredTerm;
import sjtu.ipads.wtune.prover.expr.SumExpr;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;
import sjtu.ipads.wtune.prover.expr.UninterpretedPredTerm;
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
          new MulAssociativity(),
          new AddAssociativity(),
          new Distribution());
  // Section 3.3, Eq 1-9
  private static final List<Transformation> PASS1 = List.of(new MulSum(), new SumSum());
  private static final List<Transformation> PASS2 =
      List.of(
          new SquashCommunity(), new MulAssociativity(), new AddAssociativity(), new MulSquash());
  private static final List<Transformation> PASS3 =
      List.of(new NotCommunity(), new MulAssociativity(), new AddAssociativity(), new MulNot());

  private final Proof proof;

  Normalization(Proof proof) {
    this.proof = coalesce(proof, Proof.makeNull());
  }

  public static Disjunction normalize(UExpr root) {
    return normalize(root, true, NORMALIZATION_VAR_PREFIX);
  }

  public static Disjunction normalize(UExpr root, boolean splitVars, String renamePrefix) {
    final Disjunction d =
        asDisjunction(new Normalization(null).transform(root.copy()), splitVars, new Counter());
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
        final UExpr applied = tf.apply(target, proof);
        if (applied != target) {
          return transform(UExpr.postorderTraversal(rootOf(applied)), tfs);
        }
      }

    return head(targets);
  }

  private static Disjunction asDisjunction(UExpr root, boolean shouldSplitVars, Counter counter) {
    final List<UExpr> factors = listFactors(root, Kind.ADD);
    return new DisjunctionImpl(listMap(it -> asConjunction(it, shouldSplitVars, counter), factors));
  }

  private static Conjunction asConjunction(UExpr root, boolean shouldSplitVars, Counter counter) {
    final UExpr expr;
    final List<Tuple> oldVars;
    if (root.kind() == Kind.SUM) {
      expr = root.child(0);
      oldVars = ((SumExpr) root).boundedVars();
    } else {
      expr = root;
      oldVars = emptyList();
    }

    if (expr.kind() == Kind.SUM) throw new IllegalArgumentException("not a normal form: " + root);

    final List<Tuple> boundedVars =
        shouldSplitVars ? splitVariables(expr, oldVars, counter) : oldVars;
    final List<UExpr> factors = listFactors(expr, Kind.MUL);
    final List<UExpr> tables = listFilter(it -> it.kind() == Kind.TABLE, factors);
    final List<UExpr> sqs = listFilter(it -> it.kind() == Kind.SQUASH, factors);
    final List<UExpr> negs = listFilter(it -> it.kind() == Kind.NOT, factors);
    final List<UExpr> predicates = listFilter(it -> it.kind().isPred(), factors);

    if (sqs.size() >= 2 || negs.size() >= 2)
      throw new IllegalArgumentException("not a normal form: " + root);

    return Conjunction.make(
        boundedVars,
        tables,
        predicates,
        sqs.isEmpty() ? null : asDisjunction(sqs.get(0).child(0), shouldSplitVars, counter),
        negs.isEmpty() ? null : asDisjunction(negs.get(0).child(0), shouldSplitVars, counter));
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

  private static List<Tuple> splitVariables(UExpr expr, List<Tuple> oldVars, Counter counter) {
    final Set<Tuple> vars = collectVarComponents(expr, oldVars);
    final List<Tuple> variables = new ArrayList<>(vars.size());
    final Set<Tuple> split = new HashSet<>(oldVars.size());

    for (Tuple oldVar : vars) {
      final Tuple newVar = Tuple.make(TEMP_VAR_PREFIX_0 + counter.addAssign());

      expr.subst(oldVar, newVar);
      variables.add(newVar);
      split.add(oldVar.base()[0]);
    }

    if (split.size() != oldVars.size())
      for (Tuple originalVar : oldVars)
        if (!split.contains(originalVar)) variables.add(originalVar);

    return variables;
  }

  private static Set<Tuple> collectVarComponents(UExpr expr, List<Tuple> interestSet) {
    final VarComponentCollector collector = new VarComponentCollector(interestSet);
    collector.onNode(expr);
    return collector.components;
  }

  // given a set of tuple `V` and an U-expr `expr`,
  // returns {t | t in `expr` /\ v in `V` /\ t.uses(v)}.
  private static class VarComponentCollector {
    private final List<Tuple> interestSet;
    private final Set<Tuple> components;
    private final List<Tuple> screened;

    private VarComponentCollector(List<Tuple> interestSet) {
      if (!all(interestSet, Tuple::isBase)) throw new IllegalArgumentException();

      this.interestSet = interestSet;
      this.components = new HashSet<>();
      this.screened = new ArrayList<>();
    }

    private void onNode(UExpr expr) {
      if (expr.kind() == Kind.SUM) screened.addAll(((SumExpr) expr).boundedVars());

      for (UExpr child : expr.children()) onNode(child);

      if (expr.kind() == Kind.SUM) {
        final SumExpr sum = (SumExpr) expr;
        screened.subList(screened.size() - sum.boundedVars().size(), screened.size()).clear();
      }

      switch (expr.kind()) {
        case TABLE:
          extractComponent(((TableTerm) expr).tuple());
          break;
        case PRED:
          for (Tuple tuple : ((UninterpretedPredTerm) expr).tuple()) extractComponent(tuple);
          break;
        case EQ_PRED:
          extractComponent(((EqPredTerm) expr).left());
          extractComponent(((EqPredTerm) expr).right());
          break;
      }
    }

    private void extractComponent(Tuple t) {
      if (t.isProjected()) {
        final Tuple base = t.base()[0];
        if (base.isProjected() || base.isFunc()) extractComponent(base);
        else if (base.isBase() && !screened.contains(base) && interestSet.contains(base))
          components.add(t);

      } else if (t.isFunc()) {
        for (Tuple arg : t.base()) extractComponent(arg);
      }
    }
  }
}
