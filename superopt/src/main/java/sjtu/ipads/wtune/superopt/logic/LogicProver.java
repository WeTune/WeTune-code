package sjtu.ipads.wtune.superopt.logic;

import com.microsoft.z3.*;
import sjtu.ipads.wtune.superopt.uexpr.UExprTranslationResult;

import static sjtu.ipads.wtune.superopt.logic.LogicSupport.*;

class LogicProver {
  static {
    //    Global.setParameter("smt.auto_config", "false");
    Global.setParameter("smt.random_seed", "112358");
    Global.setParameter("smt.qi.quick_checker", "2");
    Global.setParameter("smt.qi.max_multi_patterns", "1024");
    Global.setParameter("smt.mbqi.max_iterations", "3");
    Global.setParameter("timeout", System.getProperty("wetune.smt_timeout", "50"));
    Global.setParameter("combined_solver.solver2_unknown", "2");
    Global.setParameter("pp.max_depth", "100");
  }

  private final UExprTranslationResult uExprs;

  LogicProver(UExprTranslationResult uExprs) {
    this.uExprs = uExprs;
  }

  int proveEq() {
    try (final Context z3 = new Context()) {
      final LogicTranslator translator = new LogicTranslator(uExprs, z3);
      if (!translator.translate()) return UNKNOWN; // treat as conflict

      final Solver solver = z3.mkSolver();
      solver.add(translator.assertions().toArray(BoolExpr[]::new));
      final Status result = solver.check();

      if (result == Status.UNSATISFIABLE) return EQ;
      else if (result == Status.SATISFIABLE) return NEQ;
      else return UNKNOWN;
    }
  }
}
