package sjtu.ipads.wtune.sqlparser.ast.constants;

public enum WindowUnit {
  ROWS,
  RANGE,
  GROUPS;

  private final String text = name();

  public String text() {
    return text;
  }
}
