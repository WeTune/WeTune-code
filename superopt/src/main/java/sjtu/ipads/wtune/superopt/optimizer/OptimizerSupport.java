package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sql.plan.PlanContext;

public abstract class OptimizerSupport {
  public static final String FAILURE_INCOMPLETE_MODEL = "incomplete model ";
  public static final String FAILURE_MISMATCHED_JOIN_KEYS = "mismatched join key ";
  public static final String FAILURE_FOREIGN_VALUE = "foreign value ";
  public static final String FAILURE_UNKNOWN_OP = "unknown op ";

  private static final ThreadLocal<String> LAST_ERROR = new ThreadLocal<>();

  static final System.Logger LOG = System.getLogger("optimizer");

  static void setLastError(String error) {
    LAST_ERROR.set(error);
  }

  static int normalizePlan(PlanContext plan, int rootId) {
    rootId = new NormalizeJoin(plan).normalizeTree(rootId);
    rootId = new NormalizeProj(plan).normalizeTree(rootId);
    return rootId;
  }

  public static String getLastError() {
    return LAST_ERROR.get();
  }
}
