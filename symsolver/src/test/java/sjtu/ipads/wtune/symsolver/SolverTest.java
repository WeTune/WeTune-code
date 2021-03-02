package sjtu.ipads.wtune.symsolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.queries.*;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Tracer;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.common.utils.Commons.asArray;
import static sjtu.ipads.wtune.symsolver.DecidableConstraint.*;

public class SolverTest {
  @Test
  @DisplayName("[prove] remove inner join")
  void testProof0() {
    final QueryBuilder q0 = new InnerJoinQuery();
    final QueryBuilder q1 = new SimpleProjQuery();

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

  @Test
  @DisplayName("[prove] subquery to inner join")
  void testProof1() {
    final QueryBuilder q0 = new InnerJoinQuery();
    final QueryBuilder q1 = new InSubqueryQuery();

    final Solver solver = Solver.make(q0, q1);
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();

    final Decision[] constraints0 =
        asArray(
            tableEq(tables[0], tables[2]),
            tableEq(tables[1], tables[3]),
            pickEq(picks[0], picks[3]),
            pickEq(picks[1], picks[4]),
            pickEq(picks[2], picks[5]),
            pickFrom(picks[0], tables[0]));
    assertSame(Result.EQUIVALENT, solver.check(constraints0));

    solver.close();
  }

  @Test
  @DisplayName("[prove] filter push up")
  void testProof2() {
    final QueryBuilder q0 = new JoinFilterQuery();
    final QueryBuilder q1 = new FilterJoinQuery();

    final Solver solver = Solver.make(q0, q1);
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();
    final PredicateSym[] preds = solver.predicates();

    final Decision[] constraints0 =
        asArray(
            tableEq(tables[0], tables[2]),
            tableEq(tables[1], tables[3]),
            pickEq(picks[0], picks[5]),
            pickEq(picks[1], picks[6]),
            pickEq(picks[2], picks[7]),
            pickEq(picks[3], picks[4]),
            predicateEq(preds[0], preds[1]),
            pickFrom(picks[4], asArray(tables[3])),
            pickSub(picks[3], picks[2]));

    System.out.println(solver.check(constraints0));
    assertSame(Result.EQUIVALENT, solver.check(constraints0));

    solver.close();
  }

  @Test
  void testConflict() {
    final QueryBuilder q0 = new InnerJoinQuery();
    final QueryBuilder q1 = new SimpleProjQuery();

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
    final QueryBuilder q0 = new InnerJoinQuery();
    final QueryBuilder q1 = new SimpleProjQuery();

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
