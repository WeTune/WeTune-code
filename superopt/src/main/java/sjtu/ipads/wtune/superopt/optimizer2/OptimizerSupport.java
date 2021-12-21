package sjtu.ipads.wtune.superopt.optimizer2;

public interface OptimizerSupport {
  int FAILURE_INCOMPLETE_MODEL = 0;
  int FAILURE_MISMATCHED_JOIN_KEYS = 1;
  int FAILURE_FOREIGN_VALUE = 2;
  int FAILURE_BAD_PROJECTION = 3;
  int FAILURE_UNKNOWN_OP = 4;
}
