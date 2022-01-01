package sjtu.ipads.wtune.superopt.optimizer2;

public abstract class OptimizerSupport {
  public static final String FAILURE_INCOMPLETE_MODEL = "incomplete model ";
  public static final String FAILURE_MISMATCHED_JOIN_KEYS = "mismatched join key ";
  public static final String FAILURE_FOREIGN_VALUE = "foreign value ";
  public static final String FAILURE_UNKNOWN_OP = "unknown op ";

  private static final ThreadLocal<String> LAST_ERROR = new ThreadLocal<>();

  static void setLastError(String error) {
    LAST_ERROR.set(error);
  }

  public static String getLastError() {
    return LAST_ERROR.get();
  }
}
