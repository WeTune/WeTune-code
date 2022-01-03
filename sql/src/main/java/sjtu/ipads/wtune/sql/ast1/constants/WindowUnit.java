package sjtu.ipads.wtune.sql.ast1.constants;

public enum WindowUnit {
  ROWS,
  RANGE,
  GROUPS;

  private final String text = name();

  public String text() {
    return text;
  }
}
