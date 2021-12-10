package sjtu.ipads.wtune.superopt.logic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Global;
import sjtu.ipads.wtune.superopt.uexpr.UExprTranslationResult;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class LogicSupport {
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

  public static final int EQ = 1, NEQ = -1, UNKNOWN = 0, FAST_REJECTED = 2;
  private static final AtomicInteger NUM_INVOCATIONS = new AtomicInteger(0);

  private LogicSupport() {}

  static void incrementNumInvocations() {
    NUM_INVOCATIONS.incrementAndGet();
  }

  public static int numInvocations() {
    return NUM_INVOCATIONS.get();
  }

  public static int proveEq(UExprTranslationResult uExprs) {
    try (final Context z3 = new Context()) {
      return new LogicProver(uExprs, z3).proveEq();
    }
  }
}
