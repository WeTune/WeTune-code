package sjtu.ipads.wtune.common.utils;

public class IgnorableException extends RuntimeException {
  private final boolean ignorable;

  public IgnorableException(String cause, boolean ignorable) {
    super(cause);
    this.ignorable = ignorable;
  }

  public boolean ignorable() {
    return ignorable;
  }
}
