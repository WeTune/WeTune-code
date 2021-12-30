package sjtu.ipads.wtune.superopt.optimizer2;

import sjtu.ipads.wtune.superopt.fragment.Op;

public abstract class OptimizerSupport {
  public static final String FAILURE_INCOMPLETE_MODEL = "incomplete model ";
  public static final String FAILURE_MISMATCHED_JOIN_KEYS = "mismatched join key ";
  public static final String FAILURE_FOREIGN_VALUE = "foreign value ";
  public static final String FAILURE_UNKNOWN_OP = "unknown op ";

  private static final ThreadLocal<String> LAST_ERROR = new ThreadLocal<>();

  static Op predecessorOfFilters(Op filterHead) {
    Op cursor = filterHead;
    while (cursor.kind().isFilter()) cursor = cursor.predecessors()[0];
    return cursor;
  }

  static void setLastError(String error) {
    LAST_ERROR.set(error);
  }

  public static String getLastError() {
    return LAST_ERROR.get();
  }
}
