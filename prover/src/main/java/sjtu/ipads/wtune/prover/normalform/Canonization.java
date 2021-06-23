package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.common.utils.FuncUtils.all;
import static sjtu.ipads.wtune.common.utils.FuncUtils.any;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.prover.ProverSupport.normalize;
import static sjtu.ipads.wtune.prover.expr.UExpr.suffixTraversal;
import static sjtu.ipads.wtune.prover.utils.Util.ownerTableOf;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;
import sjtu.ipads.wtune.prover.utils.Congruence;
import sjtu.ipads.wtune.prover.utils.Counter;
import sjtu.ipads.wtune.prover.utils.TupleCongruence;
import sjtu.ipads.wtune.prover.utils.Util;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;

class Canonization {
  // Definition 4.4: S(t) -> S(t) * Sum[t'](R(t') * [t.k=t'.k'], where S.k=>R.k' is a FK
  // Note: to be called before `Normalization::transform`
  static UExpr applyForeignKey(UExpr expr, Collection<Constraint> foreignKeys) {
    final List<UExpr> tables = listFilter(it -> it.kind() == Kind.TABLE, suffixTraversal(expr));
    final Counter counter = new Counter();

    for (UExpr tableTerm : tables) {
      final TableTerm table = (TableTerm) tableTerm;
      final String tableName = table.name().toString();
      final UExpr term =
          foreignKeys.stream()
              .filter(it -> tableName.equals(ownerTableOf(it)))
              .map(it -> makeForeignKeyTerm(table, it, counter))
              .reduce(table.copy(), UExpr::mul);

      UExpr.replaceChild(table.parent(), table, term);
    }

    return expr;
  }

  // Note: to be called after `Normalization::transform`
  static Disjunction applyUniqueKey(Disjunction d, Collection<Constraint> uks) {
    d = d.copy();
    applyUk1(d, uks);
    applyUk2(d, uks);
    return d;
  }

  private static void applyUk1(Disjunction d, Collection<Constraint> uks) {
    final List<Conjunction> conjunctions = d.conjunctions();
    for (int i = 0, bound = conjunctions.size(); i < bound; i++) {
      final Conjunction conjunction = applyUk1(conjunctions.get(i), uks);
      conjunctions.set(i, conjunction);
    }
  }

  private static void applyUk2(Disjunction d, Collection<Constraint> uks) {
    final List<Conjunction> conjunctions = d.conjunctions();
    for (int i = 0, bound = conjunctions.size(); i < bound; i++) {
      final Conjunction conjunction = applyUk2(conjunctions.get(i), uks);
      conjunctions.set(i, conjunction);
    }
  }

  // Definition 4.1: [t.k=t'.k] * R(t) * R(t') -> [t=t'] * R(t), where R.k is a UK
  private static Conjunction applyUk1(Conjunction conjunction, Collection<Constraint> uks) {
    final Map<String, List<TableTerm>> groups = Util.groupTables(conjunction);
    final Congruence<Tuple> cong = TupleCongruence.make(conjunction.predicates());
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
    if (conjunction.negation() != null) applyUk1(conjunction.negation(), uks);

    return conjunction;
  }

  // Theorem 4.3: Sum[t]([b] * |E| * [t.k=e] * R(t)) -> |Sum[t]([b] * |E| * [t.k=e] * R(t))|,
  // where R.k is a UK
  private static Conjunction applyUk2(Conjunction conjunction, Collection<Constraint> uks) {
    final Map<String, List<TableTerm>> groups = Util.groupTables(conjunction);
    final Congruence<Tuple> cong = TupleCongruence.make(conjunction.predicates());

    /*
     NOTE: Theorem 4.3 cannot generalize to multiple tables.
       e.g. SELECT T.k, S.k' FROM T JOIN S
         => Sum[t1,t2]([t.k=t1.k] * [t.k'=t2.k'] * R(t1) * S(t2))
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
            c -> any(cong.getClass(term.tuple().proj(c.name())), Canonization::isConstantTuple))) {
          found = true;
          break outer;
        }
    }

    if (!found) return conjunction;

    // Surround the conjunction with squash.
    // This process may subsequently eliminate a inner squash.
    // We just delegate it to `normalize`
    final Disjunction disjunction = normalize(UExpr.squash(conjunction.toExpr()), null);
    // The resulting SPNF must be in the form |E|.
    if (disjunction.conjunctions().size() != 1)
      throw new IllegalStateException("invalid conjunction: " + conjunction);

    return disjunction.conjunctions().get(0);
  }

  private static UExpr makeForeignKeyTerm(TableTerm table, Constraint foreignKey, Counter counter) {
    final List<Column> refColumns = foreignKey.refColumns();
    final List<? extends Column> columns = foreignKey.columns();

    final String refTable = refColumns.get(0).tableName();
    final Tuple boundedTuple = Tuple.make("m" + counter.next());
    final Tuple tuple = table.tuple();

    final List<UExpr> eqPreds = new ArrayList<>(columns.size() + 1);
    for (int i = 0, bound = refColumns.size(); i < bound; ++i) {
      final Column column = columns.get(i);
      final Column refColumn = refColumns.get(i);
      eqPreds.add(UExpr.eqPred(tuple.proj(column.name()), boundedTuple.proj(refColumn.name())));
    }
    eqPreds.add(UExpr.table(refTable, boundedTuple));

    final UExpr expr = eqPreds.stream().reduce(UExpr::mul).orElse(null);
    return UExpr.sum(boundedTuple, expr);
  }

  private static boolean isConstantTuple(Tuple t) {
    if (t.isConstant()) return true;
    if (t.isBase() && t.name().toString().equals("t")) return true;
    if (t.isProjected()) return isConstantTuple(t.base()[0]);
    return false;
  }
}
