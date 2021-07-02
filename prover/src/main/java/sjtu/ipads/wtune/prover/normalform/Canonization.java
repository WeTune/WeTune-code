package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.common.utils.FuncUtils.all;
import static sjtu.ipads.wtune.common.utils.FuncUtils.any;
import static sjtu.ipads.wtune.common.utils.FuncUtils.find;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.prover.ProverSupport.normalizeExpr;
import static sjtu.ipads.wtune.prover.utils.Constants.EXTRA_VAR_PREFIX;
import static sjtu.ipads.wtune.prover.utils.Constants.TRANSLATOR_VAR_PREFIX;
import static sjtu.ipads.wtune.prover.utils.Util.isConstantTuple;
import static sjtu.ipads.wtune.prover.utils.Util.isNotNullPredOf;
import static sjtu.ipads.wtune.prover.utils.Util.isNullTuple;
import static sjtu.ipads.wtune.prover.utils.Util.ownerTableOf;
import static sjtu.ipads.wtune.prover.utils.Util.renameVars;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import sjtu.ipads.wtune.prover.DecisionContext;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.utils.Congruence;
import sjtu.ipads.wtune.prover.utils.Util;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;

public final class Canonization {
  private Canonization() {}

  public static Disjunction canonize(Disjunction d, DecisionContext ctx) {
    applyConstant(d);
    applyEqualRelation(d);
    applyUniqueKey(d, ctx.uniqueKeys());
    applyForeignKey(d, ctx.foreignKeys());
    renameVars(d, TRANSLATOR_VAR_PREFIX);
    return ExcludedMiddle.collapse(d);
  }

  private static void applyConstant(Disjunction d) {
    applyToEachConjunction(d, Canonization::applyConst1);
    applyToEachConjunction(d, Canonization::applyConst2);
  }

  // [x = const_val] * f(x) -> [x = const_val] * f(const_val)
  private static void applyConst1(Disjunction d) {
    applyToEachConjunction(d, Canonization::applyConst1);
  }

  // [x = const_val] * f(x) -> [x = const_val] * f(const_val)
  // [x not Null] * [x = const] -> [x = const]
  private static Conjunction applyConst1(Conjunction c) {
    final Congruence<Tuple> congruence = Congruence.make(c.preds());

    final Map<Tuple, Tuple> toSubstVar = new HashMap<>();
    final Set<UExpr> toRemovePred = new HashSet<>();

    for (Tuple val : congruence.keys()) {
      if (!isConstantTuple(val)) continue;
      final Set<Tuple> cls = congruence.getClass(val);

      for (Tuple t : cls) if (t.isBase()) toSubstVar.put(t, val);
      if (isNullTuple(val)) continue;

      for (UExpr pred : c.preds())
        if (find(it -> isNotNullPredOf(pred, it), cls) != null) {
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

  private static void applyConst2(Disjunction d) {
    applyToEachConjunction(d, Canonization::applyConst2);
  }

  // [x = Null] * f(x.y) -> [x = Null] * f(y)
  private static Conjunction applyConst2(Conjunction c) {
    c.preds().forEach(NullPropagator::propagateNull);
    c.tables().forEach(NullPropagator::propagateNull);
    if (c.squash() != null) applyConst2(c.squash());
    if (c.neg() != null) applyConst2(c.neg());
    return c;
  }

  private static void applyEqualRelation(Disjunction d) {
    applyToEachConjunction(d, Canonization::applyEqRel);
  }

  private static Conjunction applyEqRel(Conjunction c) {
    if (any(c.preds(), Util::isContradictory)) return null;
    c.preds().removeIf(Util::isReflexivity);

    if (c.squash() != null) applyToEachConjunction(c.squash(), Canonization::applyEqRel);
    if (c.squash() != null) applyToEachConjunction(c.squash(), Canonization::applyEqRel);

    return c;
  }

  private static void applyUniqueKey(Disjunction d, Collection<Constraint> uks) {
    if (uks.isEmpty()) return;
    applyUk1(d, uks);
    applyUk2(d, uks);
  }

  private static void applyUk1(Disjunction d, Collection<Constraint> uks) {
    applyToEachConjunction(d, c -> applyUk1(c, uks));
  }

  private static void applyUk2(Disjunction d, Collection<Constraint> uks) {
    applyToEachConjunction(d, c -> applyUk2(c, uks));
  }

  // Definition 4.1: [t.uk=t'.uk] * R(t) * R(t') -> [t=t'] * R(t)
  private static Conjunction applyUk1(Conjunction conjunction, Collection<Constraint> uks) {
    final Map<String, List<TableTerm>> groups = Util.groupTables(conjunction);
    final Congruence<Tuple> cong = Congruence.make(conjunction.preds());
    final Map<TableTerm, List<TableTerm>> victims = new HashMap<>();

    for (Constraint uniqueKey : uks) {
      final List<? extends Column> columns = uniqueKey.columns();
      final List<TableTerm> tables = groups.get(columns.get(0).tableName());
      // 1. find R such that there are R(t0) * R(t1) * R(t2) * ... in the Conjunction
      if (tables == null || tables.size() <= 1) continue;
      // 2. find [t0.k=tn.k] for each <t0,tn> where n >= 1
      //    generalization (multi-column unique key):
      //    find [t0.k0=tn.k0] * [t0.k1=tn.k1] * ... * [t0.kj=tn.kj]
      final TableTerm pivot = tables.get(0);
      for (TableTerm term : tables.subList(1, tables.size()))
        if (all(
            columns,
            c -> cong.isCongruent(pivot.tuple().proj(c.name()), term.tuple().proj(c.name()))))
          victims.computeIfAbsent(pivot, k -> new ArrayList<>()).add(term);
    }

    final List<UExpr> tables = conjunction.tables();
    // 3. remove R(tn), substitute tn with t0
    for (var pair : victims.entrySet()) {
      final TableTerm pivot = pair.getKey();
      final List<TableTerm> victim = pair.getValue();
      tables.removeAll(victim);
      victim.forEach(it -> conjunction.subst(pivot.tuple(), it.tuple()));
    }

    // recurse on Negation and Squash term
    if (conjunction.squash() != null) applyUk1(conjunction.squash(), uks);
    if (conjunction.neg() != null) applyUk1(conjunction.neg(), uks);

    return conjunction;
  }

  // Theorem 4.3: Sum[t]([b] * |E| * [t.uk=e] * R(t)) -> |Sum[t]([b] * |E| * [t.uk=e] * R(t))|,
  private static Conjunction applyUk2(Conjunction conjunction, Collection<Constraint> uks) {
    final Map<String, List<TableTerm>> groups = Util.groupTables(conjunction);
    final Congruence<Tuple> cong = Congruence.make(conjunction.preds());

    /*
     NOTE: Theorem 4.3 cannot generalize to multiple tables.
       e.g. SELECT T.k, S.k' FROM T JOIN S
         => Sum[t1,t2]([t.k=t1.k] * [t.k=t2.k'] * R(t1) * S(t2))
       Provided T.k and S.k' are the unique keys of each table.
       There is no way to apply the theorem.
       (Derive a combined unique key <T.k,S.k'> is out of scope of the theorem)

       Btw, WeTune can easily inference the uniqueness of the query

       Besides, the "e" in the theorem must be a constant value.
       Otherwise, a term such as [t.k=t.k] apparently takes no effect.
    */
    if (groups.size() != 1) return conjunction;

    final String tableName = Iterables.getOnlyElement(groups.keySet());
    final List<TableTerm> terms = groups.get(tableName); // R(t0),R(t1),...

    boolean found = false;
    outer:
    // find a R(tn) and a unique key constraint (k1, k2, ..., kj)
    // such that there is [tn.k1=e1] * [tn.k2=e2] * ... * [tn.kj=ej],
    // where kj is the j-th column of the unique key and ej is a constant value.
    for (Constraint uniqueKey : uks) {
      if (!tableName.equals(uniqueKey.columns().get(0).tableName())) continue;
      for (TableTerm term : terms)
        if (all(
            uniqueKey.columns(),
            c -> any(cong.getClass(term.tuple().proj(c.name())), Util::isConstantTuple))) {
          found = true;
          break outer;
        }
    }

    if (!found) return conjunction;

    // Surround the conjunction with squash.
    // This process may subsequently eliminate a inner squash.
    // We just delegate it to `normalize`
    final Disjunction disjunction = normalizeExpr(UExpr.squash(conjunction.toExpr()));
    // The resulting SPNF must be in the form |E|.
    if (disjunction.conjunctions().size() != 1)
      throw new IllegalStateException("invalid conjunction: " + conjunction);

    return disjunction.conjunctions().get(0);
  }

  private static void applyForeignKey(Disjunction d, Collection<Constraint> fks) {
    if (fks.isEmpty()) return;

    //    applyToEachConjunction(d, c -> applyFk1(c, fks));
    applyFk2(d, fks);
  }

  //  // Additional definition: S(t) * not(Sum[t'](R(t') * [t.k=t'.k']) -> 0, where S.k=>R.t' is a
  // FK
  //  private static Conjunction applyFk1(Conjunction c, Collection<Constraint> fks) {
  //    final Disjunction negation = c.negation();
  //    if (negation == null || c.tables().isEmpty()) return c;
  //
  //    final Set<Pair<String, String>> fkRelations = new HashSet<>();
  //    for (Constraint fk : fks)
  //      fkRelations.add(Pair.of(ownerTableOf(fk), fk.refColumns().get(0).tableName()));
  //
  //    boolean found = false;
  //    loop:
  //    for (Conjunction innerC : negation.conjunctions()) {
  //      if (innerC.boundedVars().size() != 1 || innerC.tables().size() != 1) continue;
  //      for (var pair : Lists.cartesianProduct(c.tables(), innerC.tables())) {
  //        final String outerTable = ((TableTerm) pair.get(0)).name().toString();
  //        final String innerTable = ((TableTerm) pair.get(1)).name().toString();
  //        if (fkRelations.contains(Pair.of(outerTable, innerTable))) {
  //          found = true;
  //          break loop;
  //        }
  //      }
  //    }
  //
  //    return found ? null : c;
  //  }

  private static void applyFk2(Disjunction d, Collection<Constraint> fks) {
    applyToEachConjunction(d, c -> applyFk2(c, fks));
  }

  // Definition 4.4: S(t) -> S(t) * Sum[t'](R(t') * [t.k=t'.k'], where S.k=>R.k' is a FK
  private static Conjunction applyFk2(Conjunction c, Collection<Constraint> fks) {
    final List<Tuple> vars = c.vars();
    final List<UExpr> predicates = c.preds();
    final List<UExpr> tables = c.tables();

    int idx = 0;
    for (int i = 0, bound = tables.size(); i < bound; ++i) {
      final TableTerm table = (TableTerm) tables.get(i);
      final String tableName = table.name().toString();
      final Tuple tuple = table.tuple();

      for (Constraint fk : listFilter(it -> tableName.equals(ownerTableOf(it)), fks)) {
        final String refTableName = fk.refColumns().get(0).tableName();
        final List<? extends Column> columns = fk.columns();
        final List<Column> refColumns = fk.refColumns();

        final Tuple newTuple = Tuple.make(EXTRA_VAR_PREFIX + idx++);
        final UExpr newTable = UExpr.table(refTableName, newTuple);

        vars.add(newTuple);
        tables.add(newTable);

        for (int j = 0, limit = columns.size(); j < limit; ++j) {
          final Column col = columns.get(j);
          final Column refCol = refColumns.get(j);
          predicates.add(UExpr.eqPred(tuple.proj(col.name()), newTuple.proj(refCol.name())));
        }
      }
    }

    if (c.squash() != null) applyFk2(c.squash(), fks);
    if (c.neg() != null) applyFk2(c.neg(), fks);

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
