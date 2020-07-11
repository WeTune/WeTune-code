package sjtu.ipads.wtune.stmt.attrs;

public class Param {
  enum Modifier {
    EQ,
    GT,
    GE,
    LT,
    LE
  }

  private ColumnRef columnRef;

  public ColumnRef columnRef() {
    return columnRef;
  }

  public Param setColumnRef(ColumnRef columnRef) {
    this.columnRef = columnRef;
    return this;
  }
}
