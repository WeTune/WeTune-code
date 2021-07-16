package sjtu.ipads.wtune.prover.logic;

import static sjtu.ipads.wtune.prover.logic.Z3LogicCtx.unwrap;

import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;

class Z3Solver implements LogicSolver {
  private final Solver z3Solver;

  private Z3Solver(Solver z3Solver) {
    this.z3Solver = z3Solver;
  }

  static LogicSolver mk(Solver z3Solver) {
    return new Z3Solver(z3Solver);
  }

  private static Result convertResult(Status status) {
    switch (status) {
      case UNKNOWN:
        return Result.UNKNOWN;
      case SATISFIABLE:
        return Result.SAT;
      case UNSATISFIABLE:
        return Result.UNSAT;
      default:
        throw new IllegalStateException();
    }
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
