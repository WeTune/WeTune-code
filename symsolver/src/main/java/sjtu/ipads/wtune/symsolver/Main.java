package sjtu.ipads.wtune.symsolver;

import sjtu.ipads.wtune.common.utils.ISupplier;
import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;
import sjtu.ipads.wtune.symsolver.utils.SimpleScoped;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.collect.Sets.powerSet;
import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.common.utils.Commons.asArray;
import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;
import static sjtu.ipads.wtune.symsolver.DecidableConstraint.*;

public class Main {
  public static void main(String[] args) {
    test();
  }

  private static void test() {
    final QueryBuilder q0 = new Query0();
    final QueryBuilder q1 = new Query1();

    final Solver solver = Solver.make(q0, q1);
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();
    final PredicateSym[] preds = solver.predicates();

    final DecisionTree tree =
        DecisionTree.from(
            //            tableEq(tables[0], tables[1]),
            tableEq(tables[0], tables[2]),
            //            tableEq(tables[1], tables[2]),
            //            pickEq(picks[0], picks[1]),
            //            pickEq(picks[0], picks[2]),
            pickEq(picks[0], picks[3]),
            //            pickEq(picks[0], picks[4]),
            //            pickEq(picks[1], picks[2]),
            //            pickEq(picks[3], picks[5]),
            //            pickEq(picks[2], picks[3]),
            pickFrom(picks[0], tables[0]),
            //            DecidableConstraint.pickFrom(picks[3], tables[0]),
            //            pickFrom(picks[0], tables[1]),
            //            pickFrom(picks[3], tables[0]),
            //            pickFrom(picks[0], tables[1]),
            //            pickFrom(picks[0], tables[0], tables[1]),
            //            pickFrom(picks[1], tables[0]),
            //            pickFrom(picks[2], tables[0]),
            //            predicateEq(preds[0], preds[1]),
            reference(tables[0], picks[1], tables[1], picks[2]));

    //    System.out.println(solver.check(tree.choices()));
    //    final Collection<Summary> summaries = solver.solve(tree);
    final Collection<Summary> summaries = solver.solve();

    for (Summary summary : summaries) {
      System.out.println(summary);
      System.out.println(Arrays.toString(summary.constraints()));
    }

    solver.close();
  }

  private static class Query0 extends BaseQueryBuilder {
    @Override
    protected Function<Value, Proposition> semantic() {
      final Object scope = new Object();
      final ISupplier<Scoped> supplier = () -> new SimpleScoped(scope);

      final TableSym[] tables = arrayMap(this::tableSym, TableSym.class, supplier.repeat(2));
      final PickSym[] picks = arrayMap(this::pickSym, PickSym.class, supplier.repeat(3));

      final TableSym t0 = tables[0];
      final TableSym t1 = tables[1];
      final PickSym p0 = picks[0];
      final PickSym p1 = picks[1];
      final PickSym p2 = picks[2];

      p0.setVisibleSources(asArray(t0, t1));
      p1.setVisibleSources(asArray(t0));
      p2.setVisibleSources(asArray(t1));
      p0.setViableSources(powerSet(Set.of(tables)));
      p1.setViableSources(singleton(singleton(t0)));
      p2.setViableSources(singleton(singleton(t1)));

      p1.setJoined(p2);

      final Value a = newTuple(), b = newTuple();
      final Proposition from = ctx().tupleFrom(a, t0).and(ctx().tupleFrom(b, t1));
      final Proposition join = p1.apply(a).equalsTo(p2.apply(b));

      return x -> ctx().makeExists(asArray(a, b), x.equalsTo(p0.apply(a, b)).and(from).and(join));
    }
  }

  private static class Query1 extends BaseQueryBuilder {

    @Override
    protected Function<Value, Proposition> semantic() {
      final Object scope = new Object();
      final ISupplier<Scoped> supplier = () -> new SimpleScoped(scope);

      final TableSym table = tableSym(supplier.get());
      final PickSym p0 = pickSym(supplier.get());

      p0.setVisibleSources(asArray(table));
      p0.setViableSources(powerSet(Set.of(table)));

      final Value a = newTuple();
      final Proposition from = ctx().tupleFrom(a, table);

      return x -> ctx().makeExists(a, x.equalsTo(p0.apply(a)).and(from));
    }
  }

}
