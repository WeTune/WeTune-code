package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Column;

public class ColumnAttribute implements Attribute {
  private final Column column;

  private ColumnAttribute(Column column) {
    this.column = column;
  }

  public static Attribute build(Column column) {
    return new ColumnAttribute(column);
  }

  @Override
  public String name() {
    return column.name();
  }
}
