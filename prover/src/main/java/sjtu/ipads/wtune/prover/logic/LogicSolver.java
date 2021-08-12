package sjtu.ipads.wtune.prover.logic;

import com.microsoft.z3.Solver;

public interface LogicSolver {
  enum Result {
    SAT,
    UNSAT,
    UNKNOWN
  }

  void add(Iterable<Proposition> assertion);

  Result solve();

  void reset();

  static LogicSolver z3(Solver ctx) {
    return Z3Solver.mk(ctx);
  }
}
