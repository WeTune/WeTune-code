package sjtu.ipads.wtune.symsolver;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Result;
import sjtu.ipads.wtune.symsolver.core.Solver;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.core.impl.BaseQuery;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.SmtCtx;
import sjtu.ipads.wtune.symsolver.logic.Value;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Tracer;

import java.util.Set;

import static com.google.common.collect.Sets.powerSet;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.common.utils.FuncUtils.asArray;
import static sjtu.ipads.wtune.symsolver.core.Constraint.*;

public class SolverTest {

  private static class Query0 extends BaseQuery {
    private Query0() {
      super(2, 3);
      for (PickSym pick : picks) pick.setVisibleSources(tables);
      picks[0].setViableSources(powerSet(Set.of(tables)));
      picks[1].setViableSources(singleton(singleton(tables[0])));
      picks[2].setViableSources(singleton(singleton(tables[1])));
      picks[1].setJoined(picks[2]);
    }

    @Override
    public Value output(SmtCtx ctx, Value[] tuples) {
      return picks[0].apply(tuples);
    }

    @Override
    public Proposition condition(SmtCtx ctx, Value[] tuples) {
      return picks[1].apply(tuples).equalsTo(picks[2].apply(tuples));
    }
  }

  private static class Query1 extends BaseQuery {
    private Query1() {
      super(1, 1);
      picks[0].setVisibleSources(tables);
      picks[0].setViableSources(singleton(singleton(tables[0])));
    }

    @Override
    public Value output(SmtCtx ctx, Value[] tuples) {
      return picks[0].apply(tuples);
    }

    @Override
    public Proposition condition(SmtCtx ctx, Value[] tuples) {
      return ctx.makeTautology();
    }
  }

  @Test
  void testProof() {
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
    assertSame(solver.check(constraints0), Result.EQUIVALENT);

    final Decision[] constraints1 =
        asArray(
            tableEq(tables[0], tables[2]),
            pickEq(picks[0], picks[2]),
            pickEq(picks[1], picks[3]),
            pickFrom(picks[0], tables[1]),
            reference(tables[0], picks[1], tables[1], picks[2]));
    assertSame(solver.check(constraints1), Result.EQUIVALENT);
  }

  @Test
  void testConflict() {
    final Query0 q0 = new Query0();
    final Query1 q1 = new Query1();

    final Solver solver = Solver.make(q0, q1);
    final Tracer tracer = solver.tracer();
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();

    tracer.decide(pickFrom(picks[1], tables[1]));
    assertTrue(tracer.isConflict());

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
  }
}
