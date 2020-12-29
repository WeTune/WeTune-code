package sjtu.ipads.wtune.symsolver.smt;

import com.microsoft.z3.Solver;
import sjtu.ipads.wtune.symsolver.core.Result;
import sjtu.ipads.wtune.symsolver.smt.impl.z3.Z3SmtSolver;

public interface SmtSolver {
  static SmtSolver z3(Solver underlying) {
    return Z3SmtSolver.build(underlying);
  }

  void add(Proposition... assertions);

  void reset();

  Result check();

  Result checkAssumption(Proposition[] assumptions);
}
