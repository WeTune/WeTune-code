package sjtu.ipads.wtune.symsolver.smt.impl.z3;

import com.microsoft.z3.Status;
import sjtu.ipads.wtune.symsolver.smt.Proposition;
import sjtu.ipads.wtune.symsolver.smt.SmtSolver;

import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;
import static sjtu.ipads.wtune.symsolver.smt.impl.z3.Z3SmtCtx.unwrap;

public class Z3SmtSolver implements SmtSolver {
  private final com.microsoft.z3.Solver z3;

  private Z3SmtSolver(com.microsoft.z3.Solver z3) {
    this.z3 = z3;
  }

  public static SmtSolver build(com.microsoft.z3.Solver z3) {
    return new Z3SmtSolver(z3);
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
  public void add(Proposition... assertions) {
    for (Proposition assertion : assertions) z3.add(unwrap(assertion));
  }

  @Override
  public void reset() {
    z3.reset();
  }

  @Override
  public Result check() {
    return convertResult(z3.check());
  }

  @Override
  public Result checkAssumption(Proposition[] assumptions) {
    return convertResult(z3.check(arrayMap(assumptions, Z3SmtCtx::unwrap)));
  }
}
