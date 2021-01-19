package sjtu.ipads.wtune.symsolver;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.common.utils.ISupplier;
import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Tracer;
import sjtu.ipads.wtune.symsolver.utils.SimpleScoped;

import java.util.Set;
import java.util.function.Function;

import static com.google.common.collect.Sets.powerSet;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.common.utils.Commons.asArray;
import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;
import static sjtu.ipads.wtune.symsolver.DecidableConstraint.*;

public class SolverTest {

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

      final Value a = newTuple(), b = newTuple(), c = newTuple();
      final Proposition from = ctx().tupleFrom(b, t0).and(ctx().tupleFrom(c, t1));
      final Proposition join = p1.apply(b).equalsTo(p2.apply(c));
      final Proposition combine = a.equalsTo(ctx().makeCombine(b, c));
      final Proposition body = ctx().makeExists(asArray(b, c), from.and(join).and(combine));

      return x -> ctx().makeExists(a, x.equalsTo(p0.apply(a)).and(body));
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

  @Test
  void testProof0() {
    final Query0 q0 = new Query0();
    final Query1 q1 = new Query1();

    final Solver solver = Solver.make(q0, q1);
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();

    final Decision[] constraints0 =
        asArray(
            tableEq(tables[0], tables[2]),
            pickEq(picks[0], picks[3]),
            pickFrom(picks[0], tables[0]),
            reference(tables[0], picks[1], tables[1], picks[2]));
    assertSame(Result.EQUIVALENT, solver.check(constraints0));

    final Decision[] constraints1 =
        asArray(
            tableEq(tables[0], tables[2]),
            pickEq(picks[0], picks[2]),
            pickEq(picks[1], picks[3]),
            pickFrom(picks[0], tables[1]),
            reference(tables[0], picks[1], tables[1], picks[2]));
    assertSame(Result.EQUIVALENT, solver.check(constraints1));

    solver.close();
  }

  //  @Test
  void testProof1() {
    final QueryBuilder q0 = new Query0();
    final QueryBuilder q1 = new Query1();

    final Solver solver = Solver.make(q0, q1);
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();

    final Decision[] constraints0 =
        asArray(
            tableEq(tables[0], tables[2]),
            pickEq(picks[0], picks[3]),
            pickFrom(picks[0], tables[0]),
            pickFrom(picks[3], tables[2]));
    assertSame(solver.check(constraints0), Result.EQUIVALENT);

    solver.close();
  }

  @Test
  void testConflict() {
    final Query0 q0 = new Query0();
    final Query1 q1 = new Query1();

    final Solver solver = Solver.make(q0, q1);
    final Tracer tracer = solver.tracer();
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();

    tracer.decide(pickFrom(picks[0], tables[0]), pickFrom(picks[0], tables[1]));
    assertTrue(tracer.isConflict());

    tracer.decide(
        pickEq(picks[0], picks[1]), pickFrom(picks[1], tables[0]), pickFrom(picks[0], tables[1]));
    assertTrue(tracer.isConflict());

    tracer.decide(
        tableEq(tables[0], tables[1]),
        pickEq(picks[0], picks[1]),
        pickFrom(picks[1], tables[0]),
        pickFrom(picks[0], tables[1]));
    assertTrue(tracer.isConflict());

    tracer.decide(
        pickEq(picks[0], picks[3]), pickFrom(picks[0], tables[0]), pickFrom(picks[3], tables[2]));
    assertTrue(tracer.isConflict());

    tracer.decide(
        pickEq(picks[0], picks[1]), pickFrom(picks[1], tables[0]), pickFrom(picks[0], tables[0]));
    assertFalse(tracer.isConflict());

    tracer.decide(
        tableEq(tables[0], tables[2]),
        pickEq(picks[0], picks[3]),
        pickFrom(picks[0], tables[0]),
        pickFrom(picks[3], tables[2]));
    assertFalse(tracer.isConflict());
  }

  @Test
  void testIncomplete() {
    final Query0 q0 = new Query0();
    final Query1 q1 = new Query1();

    final Solver solver = Solver.make(q0, q1);
    final Tracer tracer = solver.tracer();
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();

    tracer.decide(pickEq(picks[0], picks[1]));
    assertTrue(tracer.isIncomplete());

    tracer.decide(pickEq(picks[0], picks[1]), pickFrom(picks[3], tables[2]));
    assertTrue(tracer.isIncomplete());

    solver.close();
  }
}
