package sjtu.ipads.wtune.testbed.common;

import sjtu.ipads.wtune.sql.schema.Column;

public interface Element {
  String collectionName();

  String elementName();

  <T> T unwrap(Class<T> cls);

  static Element ofColumn(Column column) {
    return new ColumnElement(column);
  }
}
