package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.SolverImpl;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;
import sjtu.ipads.wtune.symsolver.search.Prover;
import sjtu.ipads.wtune.symsolver.search.Tracer;

import java.util.Collection;

public interface Solver extends AutoCloseable {
  TableSym[] tables();

  PickSym[] picks();

  PredicateSym[] predicates();

  Tracer tracer();

  Prover prover();

  Collection<Summary> solve();

  Collection<Summary> solve(DecisionTree tree);

  Result check(Decision... decisions);

  @Override
  void close();

  static Solver make(QueryBuilder q0, QueryBuilder q1) {
    return SolverImpl.build(q0, q1);
  }

  static Solver make(QueryBuilder q0, QueryBuilder q1, long timeout) {
    return SolverImpl.build(q0, q1, timeout);
  }
}
