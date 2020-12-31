package sjtu.ipads.wtune.symsolver.smt.impl.z3;

import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import sjtu.ipads.wtune.symsolver.core.Result;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtSolver;

import static sjtu.ipads.wtune.symsolver.smt.impl.z3.Z3SmtCtx.unwrap;

public class Z3SmtSolver implements SmtSolver {
  private final Solver z3Solver;

  private Z3SmtSolver(Solver z3Solver) {
    this.z3Solver = z3Solver;
  }

  public static SmtSolver build(Solver z3Solver) {
    return new Z3SmtSolver(z3Solver);
  }

  private static Result convertResult(Status status) {
    switch (status) {
      case UNKNOWN:
        return Result.UNKNOWN;
      case SATISFIABLE:
        return Result.NON_EQUIVALENT;
      case UNSATISFIABLE:
        return Result.EQUIVALENT;
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public void add(Proposition... assertions) {
    for (Proposition assertion : assertions) z3Solver.add(unwrap(assertion));
  }

  @Override
  public void reset() {
    z3Solver.reset();
  }

  @Override
  public Result check() {
    //    try (final var timer = new SimpleTimer()) {
    return convertResult(z3Solver.check());
    //    }
  }

  @Override
  public Result checkAssumption(Proposition[] assumptions) {
    //    try (final var timer = new SimpleTimer()) {
    return convertResult(z3Solver.check(unwrap(assumptions)));
    //    }
  }
}
