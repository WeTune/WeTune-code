package sjtu.ipads.wtune.symsolver;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Tracer;

import java.util.Set;

import static com.google.common.collect.Sets.powerSet;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.common.utils.Commons.asArray;

public class SolverTest {

  private static class Query0 extends BaseQueryBuilder {
    @Override
    public int numTables() {
      return 2;
    }

    @Override
    public int numPicks() {
      return 3;
    }

    @Override
    public int numPreds() {
      return 0;
    }

    @Override
    protected void prepare() {
      for (PickSym pick : picks) pick.setVisibleSources(tables);
      picks[0].setViableSources(powerSet(Set.of(tables)));
      picks[1].setViableSources(singleton(singleton(tables[0])));
      picks[2].setViableSources(singleton(singleton(tables[1])));
      picks[1].setJoined(picks[2]);
    }

    @Override
    public Value[] output() {
      return asArray(picks[0].apply(tuples));
    }

    @Override
    public Proposition condition() {
      return picks[1].apply(tuples).equalsTo(picks[2].apply(tuples));
    }
  }

  private static class Query1 extends BaseQueryBuilder {
    @Override
    public int numTables() {
      return 1;
    }

    @Override
    public int numPicks() {
      return 1;
    }

    @Override
    public int numPreds() {
      return 0;
    }

    @Override
    protected void prepare() {
      picks[0].setVisibleSources(tables);
      picks[0].setViableSources(singleton(singleton(tables[0])));
    }

    @Override
    public Value[] output() {
      return asArray(picks[0].apply(tuples));
    }

    @Override
    public Proposition condition() {
      return ctx().makeTautology();
    }
  }

  private static class Query2 extends BaseQueryBuilder {
    @Override
    public int numTables() {
      return 2;
    }

    @Override
    public int numPicks() {
      return 3;
    }

    @Override
    public int numPreds() {
      return 0;
    }

    @Override
    protected void prepare() {
      for (PickSym pick : picks) pick.setVisibleSources(tables);
      picks[0].setViableSources(powerSet(Set.of(tables)));
      picks[1].setViableSources(singleton(singleton(tables[0])));
      picks[2].setViableSources(singleton(singleton(tables[1])));
      picks[1].setJoined(picks[2]);
    }

    @Override
    public Value[] output() {
      final Proposition nonNulCond = picks[1].apply(tuples).equalsTo(picks[2].apply(tuples));
      final Value nullTuple = ctx().makeNullTuple();
      return asArray(picks[0].apply(tuples[0], ctx().makeIte(nonNulCond, tuples[1], nullTuple)));
    }

    @Override
    public Proposition condition() {
      return ctx().makeTautology();
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
            DecidableConstraint.tableEq(tables[0], tables[2]),
            DecidableConstraint.pickEq(picks[0], picks[3]),
            DecidableConstraint.pickFrom(picks[0], tables[0]),
            DecidableConstraint.reference(tables[0], picks[1], tables[1], picks[2]));
    assertSame(solver.check(constraints0), Result.EQUIVALENT);

    final Decision[] constraints1 =
        asArray(
            DecidableConstraint.tableEq(tables[0], tables[2]),
            DecidableConstraint.pickEq(picks[0], picks[2]),
            DecidableConstraint.pickEq(picks[1], picks[3]),
            DecidableConstraint.pickFrom(picks[0], tables[1]),
            DecidableConstraint.reference(tables[0], picks[1], tables[1], picks[2]));
    assertSame(solver.check(constraints1), Result.EQUIVALENT);

    solver.close();
  }

  @Test
  void testProof1() {
    final QueryBuilder q0 = new Query2();
    final QueryBuilder q1 = new Query1();

    final Solver solver = Solver.make(q0, q1);
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();

    final Decision[] constraints0 =
        asArray(
            DecidableConstraint.tableEq(tables[0], tables[2]),
            DecidableConstraint.pickEq(picks[0], picks[3]),
            DecidableConstraint.pickFrom(picks[0], tables[0]),
            DecidableConstraint.pickFrom(picks[3], tables[2]));
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

    tracer.decide(DecidableConstraint.pickFrom(picks[1], tables[1]));
    assertTrue(tracer.isConflict());

    tracer.decide(
        DecidableConstraint.pickFrom(picks[0], tables[0]),
        DecidableConstraint.pickFrom(picks[0], tables[1]));
    assertTrue(tracer.isConflict());

    tracer.decide(
        DecidableConstraint.pickEq(picks[0], picks[1]),
        DecidableConstraint.pickFrom(picks[1], tables[0]),
        DecidableConstraint.pickFrom(picks[0], tables[1]));
    assertTrue(tracer.isConflict());

    tracer.decide(
        DecidableConstraint.tableEq(tables[0], tables[1]),
        DecidableConstraint.pickEq(picks[0], picks[1]),
        DecidableConstraint.pickFrom(picks[1], tables[0]),
        DecidableConstraint.pickFrom(picks[0], tables[1]));
    assertTrue(tracer.isConflict());

    tracer.decide(
        DecidableConstraint.pickEq(picks[0], picks[3]),
        DecidableConstraint.pickFrom(picks[0], tables[0]),
        DecidableConstraint.pickFrom(picks[3], tables[2]));
    assertTrue(tracer.isConflict());

    tracer.decide(
        DecidableConstraint.pickEq(picks[0], picks[1]),
        DecidableConstraint.pickFrom(picks[1], tables[0]),
        DecidableConstraint.pickFrom(picks[0], tables[0]));
    assertFalse(tracer.isConflict());

    tracer.decide(
        DecidableConstraint.tableEq(tables[0], tables[2]),
        DecidableConstraint.pickEq(picks[0], picks[3]),
        DecidableConstraint.pickFrom(picks[0], tables[0]),
        DecidableConstraint.pickFrom(picks[3], tables[2]));
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

    tracer.decide(DecidableConstraint.pickEq(picks[0], picks[1]));
    assertTrue(tracer.isIncomplete());

    tracer.decide(
        DecidableConstraint.pickEq(picks[0], picks[1]),
        DecidableConstraint.pickFrom(picks[3], tables[2]));
    assertTrue(tracer.isIncomplete());

    solver.close();
  }
}
