package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.common.utils.Congruence;
import sjtu.ipads.wtune.prover.uexpr.TableTerm;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.Var;
import sjtu.ipads.wtune.prover.utils.TupleCongruence;
import sjtu.ipads.wtune.prover.utils.Util;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.*;
import java.util.function.Function;

import static sjtu.ipads.wtune.common.utils.Commons.removeIf;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.prover.normalform.Normalization.asDisjunction;
import static sjtu.ipads.wtune.prover.normalform.Normalization.normalize;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.mul;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.sum;
import static sjtu.ipads.wtune.prover.utils.Constants.TRANSLATOR_VAR_PREFIX;
import static sjtu.ipads.wtune.prover.utils.Util.*;
import static sjtu.ipads.wtune.sqlparser.schema.Constraint.filterUniqueKey;

public final class Canonization {
  private Canonization() {}

  public static Disjunction canonize(Disjunction d, Schema schema) {
    applyMinimization(d);
    applyReflexivity(d);
    applyConstant(d);
    applyUniqueKey(d, schema);
    renameVars(d, TRANSLATOR_VAR_PREFIX);
    return d;
  }

  private static void applyConstant(Disjunction d) {
    applyToEachConjunction(d, Canonization::applyConst1);
    applyToEachConjunction(d, Canonization::applyConst2);
  }

  private static void applyConst1(Disjunction d) {
    applyToEachConjunction(d, Canonization::applyConst1);
  }

  private static void applyConst2(Disjunction d) {
    applyToEachConjunction(d, Canonization::applyConst2);
  }

  private static void applyReflexivity(Disjunction d) {
    applyToEachConjunction(d, Canonization::applyRefl);
  }

  private static void applyMinimization(Disjunction d) {
    applyToEachConjunction(d, Canonization::applyMini);
  }

  private static void applyUniqueKey(Disjunction d, Schema schema) {
    applyToEachConjunction(d, c -> applyUk(c, schema));
  }

  // [x = const_val] * f(x) -> [x = const_val] * f(const_val)
  // Sum{x}[x=const_val] * f(x) -> f(const_val)
  // [x not Null] * [x = const] -> [x = const]
  private static Conjunction applyConst1(Conjunction c) {
    final Congruence<Var> congruence = TupleCongruence.mk(c.preds());

    final Map<Var, Var> toSubstVar = new HashMap<>();
    final Set<UExpr> toRemovePred = new HashSet<>();

    for (Var val : congruence.keys()) {
      if (!isConstantTuple(val)) continue;
      final Set<Var> cls = congruence.eqClassOf(val);

      for (Var t : cls) if (t.isBase()) toSubstVar.put(t, val);
      if (isNullTuple(val)) continue;

      for (UExpr pred : c.preds())
        if (find(cls, it -> isNotNullPredOf(pred, it)) != null) {
          toRemovePred.add(pred);
          break;
        }
    }

    c.vars().removeAll(toSubstVar.keySet());
    c.preds().removeAll(toRemovePred);
    toSubstVar.forEach(c::subst);

    if (c.neg() != null) applyConst1(c.neg());
    if (c.squash() != null) applyConst1(c.squash());

    return c;
  }

  // [x = Null] * f(x.y) -> [x = Null] * f(null)
  private static Conjunction applyConst2(Conjunction c) {
    c.preds().forEach(NullPropagator::propagateNull);
    c.tables().forEach(NullPropagator::propagateNull);
    if (c.squash() != null) applyConst2(c.squash());
    if (c.neg() != null) applyConst2(c.neg());
    return c;
  }

  // [x = Null] * f(x.y) -> [x = Null] * f(null)
  private static Conjunction applyRefl(Conjunction c) {
    c.preds().removeIf(Util::isReflexivity);
    if (c.squash() != null) applyConst2(c.squash());
    if (c.neg() != null) applyConst2(c.neg());
    return c;
  }

  // Theorem 4.3: Sum[t]([b] * |E| * [t.uk=e] * R(t)) -> |Sum[t]([b] * |E| * [t.uk=e] * R(t))|,
  // where [b] is all the predicates that uses `t`
  private static Conjunction applyUk(Conjunction c, Schema schema) {
    if (c.tables().isEmpty()) return c;

    final List<Var> toMoveVars = new ArrayList<>(4);
    final List<UExpr> toMoveTerms = new ArrayList<>(4);
    Congruence<Var> cong = TupleCongruence.mk(c.preds());

    final ListIterator<UExpr> iter = c.tables().listIterator();
    while (iter.hasNext()) {
      final TableTerm tableTerm = (TableTerm) iter.next();
      final Table table = schema.table(tableTerm.name().toString());

      if (table == null)
        throw new IllegalArgumentException("unknown table in term [" + tableTerm + "]");

      final Var base = tableTerm.var();
      for (Constraint uk : filterUniqueKey(table.constraints())) {
        final List<Var> vars = listMap(uk.columns(), it -> base.proj(it.name()));

        final Congruence<Var> tmp = cong;
        // Premise: \forall t \in tuples. \exists c. c == t /\ c.root() != t.root()
        // e.g. say we have UK (post.author,post.name)
        // then we will check if there is a predicate [post.author = t.x] and [post.name = s.y],
        // where t != post and s != post
        if (any(vars, tup -> none(tmp.eqClassOf(tup), tup2 -> !tup2.root().equals(base)))) continue;

        iter.remove();
        toMoveVars.add(base);
        toMoveTerms.add(tableTerm);
        // Eagerly remove the predicates and rebuild congruence.
        // e.g., we have e := Sum{a,b}([a.id=b.id] * A(a) * B(b)).
        // The final expression e' should be
        // either "Sum{a}(A(a) * |Sum{b}(B(b) * [a.id=b.id])|)"
        // or "Sum{b}(B(b) * |Sum{a}(A(a) * [a.id=b.id])|)",
        // but NOT "|Sum{a,b}([a.id=b.id] * A(a) * B(b))|"
        // Thus, we have to somehow screen [a.id=b.id] to avoid the cascade.
        toMoveTerms.addAll(removeIf(c.preds(), it -> it.uses(base)));
        cong = TupleCongruence.mk(c.preds());
      }
    }

    if (toMoveTerms.isEmpty()) return c;

    c.vars().removeAll(toMoveVars);
    c.tables().removeAll(toMoveTerms);
    // predicates has been removed

    final boolean removeNeg = c.neg() != null && any(toMoveVars, c.neg()::uses);
    if (removeNeg) toMoveTerms.add(c.neg().toExpr());

    final UExpr addedSqExpr = sum(toMoveVars, mkProduct(toMoveTerms, true));
    final Disjunction newSq;
    if (c.squash() == null) newSq = asDisjunction(addedSqExpr);
    else newSq = normalize(mul(c.squash().toExpr(), addedSqExpr), null);

    return Conjunction.mk(c.vars(), c.tables(), c.preds(), newSq, removeNeg ? null : c.neg());
  }

  // eliminate intermediate variable
  private static Conjunction applyMini(Conjunction c) {
    final List<Var> tmpVars = listFilter(c.vars(), v -> none(c.tables(), it -> it.uses(v)));
    if (tmpVars.isEmpty()) return c;

    final Congruence<Var> cong = TupleCongruence.mk(c.preds());
    for (Var key : cong.keys()) {
      final Var tmpVar = find(tmpVars, key::uses);
      if (tmpVar == null) continue;

      final Set<Var> group = cong.eqClassOf(key);
      if (group.size() == 1) continue;

      c.vars().remove(tmpVar);
      c.subst(key, find(group, it -> none(tmpVars, it::uses)));
    }

    return c;
  }

  private static void applyToEachConjunction(
      Disjunction d, Function<Conjunction, Conjunction> func) {
    final List<Conjunction> conjunctions = d.conjunctions();
    for (int i = 0, bound = conjunctions.size(); i < bound; i++) {
      final Conjunction c = func.apply(conjunctions.get(i));
      conjunctions.set(i, c);
    }
    conjunctions.removeIf(Objects::isNull);
  }
}
