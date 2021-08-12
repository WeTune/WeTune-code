package sjtu.ipads.wtune.prover.logic;

import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;

import static sjtu.ipads.wtune.prover.logic.Z3LogicCtx.unwrap;

class Z3Solver implements LogicSolver {
  private final Solver z3Solver;

  private Z3Solver(Solver z3Solver) {
    this.z3Solver = z3Solver;
  }

  static LogicSolver mk(Solver z3Solver) {
    return new Z3Solver(z3Solver);
  }

  private static Result convertResult(Status status) {
    return switch (status) {
      case UNKNOWN -> Result.UNKNOWN;
      case SATISFIABLE -> Result.SAT;
      case UNSATISFIABLE -> Result.UNSAT;
    };
  }

  @Override
  public void add(Iterable<Proposition> assertions) {
    for (Proposition assertion : assertions) z3Solver.add(unwrap(assertion));
  }

  @Override
  public void reset() {
    z3Solver.reset();
  }

  @Override
  public Result solve() {
    //    try (final var timer = new SimpleTimer()) {
    return convertResult(z3Solver.check());
    //    }
  }
}
