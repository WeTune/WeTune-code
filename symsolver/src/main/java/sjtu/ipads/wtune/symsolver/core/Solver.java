package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.SolverImpl;
import sjtu.ipads.wtune.symsolver.search.*;

import java.util.Collection;

public interface Solver {
  TableSym[] tables();

  PickSym[] picks();

  Tracer tracer();

  Prover prover();

  Collection<Summary> solve();

  Collection<Summary> solve(DecisionTree tree);

  Result check(Decision... decisions);

  static Solver make(Query q0, Query q1) {
    return SolverImpl.build(q0, q1);
  }
}
