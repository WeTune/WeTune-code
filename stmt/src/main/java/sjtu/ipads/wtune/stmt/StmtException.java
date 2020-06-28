package sjtu.ipads.wtune.stmt;

public class StmtException extends RuntimeException {
  public StmtException() {
    super();
  }

  public StmtException(String desc) {
    super(desc);
  }

  public StmtException(Throwable cause) {
    super(cause);
  }
}
