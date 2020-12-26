package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.core.impl.SolverImpl;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;

import java.util.Collection;

public interface Solver {
  Collection<Collection<Constraint>> solve();

  Collection<Collection<Constraint>> solve(DecisionTree tree);

  boolean check(Decision... decisions);

  static Solver make(Query q0, Query q1) {
    return SolverImpl.build(q0, q1);
  }
}
