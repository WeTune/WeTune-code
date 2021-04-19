package sjtu.ipads.wtune.testbed.common;

import com.google.common.base.Objects;
import sjtu.ipads.wtune.sqlparser.schema.Column;

public class ColumnElement implements Element {
  private final Column column;

  ColumnElement(Column column) {
    this.column = column;
  }

  @Override
  public String collectionName() {
    return column.tableName();
  }

  @Override
  public String elementName() {
    return column.name();
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (cls == Column.class) return (T) column;
    else return null;
  }

  @Override
  public String toString() {
    return column.tableName() + "." + column.tableName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final ColumnElement that = (ColumnElement) o;
    return Objects.equal(column, that.column);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(column);
  }
}
