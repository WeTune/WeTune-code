package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sql.plan.PlanContext;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;

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

  static int normalizeJoin(PlanContext plan, int rootId) {
    return new NormalizeJoin(plan).normalizeTree(rootId);
  }

  static int normalizeProj(PlanContext plan, int rootId) {
    return new NormalizeProj(plan).normalizeTree(rootId);
  }

  static int normalizePlan(PlanContext plan, int rootId) {
    if ((rootId = normalizeJoin(plan, rootId)) == NO_SUCH_NODE) return NO_SUCH_NODE;
    if ((rootId = normalizeProj(plan, rootId)) == NO_SUCH_NODE) return NO_SUCH_NODE;
    return rootId;
  }

  public static String getLastError() {
    return LAST_ERROR.get();
  }
}
