package sjtu.ipads.wtune.sqlparser.plan;

public class PlanException extends RuntimeException {
  public PlanException() {}

  public PlanException(String cause) {
    super(cause);
  }
}
