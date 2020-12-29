package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.SolverImpl;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;
import sjtu.ipads.wtune.symsolver.search.Summary;

import java.util.Collection;

public interface Solver {
  TableSym[] tables();

  PickSym[] picks();

  Collection<Summary> solve();

  Collection<Summary> solve(DecisionTree tree);

  Result check(Decision... decisions);

  static Solver make(Query q0, Query q1) {
    return SolverImpl.build(q0, q1);
  }
}
