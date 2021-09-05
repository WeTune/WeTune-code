package sjtu.ipads.wtune.common.utils;

public class LeveledException extends RuntimeException {
  public enum Level {
    ERROR,
    UNSUPPORTED,
    EXPECTED
  }

  private final Level level;

  public LeveledException(String cause, Level level) {
    super(cause);
    this.level = level;
  }

  public static LeveledException ignorableEx(String cause) {
    return new LeveledException(cause, Level.EXPECTED);
  }

  public static LeveledException unsupportedEx(String cause) {
    return new LeveledException(cause, Level.UNSUPPORTED);
  }

  public static boolean ignorable(Throwable ex) {
    return ex instanceof LeveledException && ((LeveledException) ex).ignorable();
  }

  public Level level() {
    return level;
  }

  public boolean ignorable() {
    return level == Level.EXPECTED || level == Level.UNSUPPORTED;
  }
}
