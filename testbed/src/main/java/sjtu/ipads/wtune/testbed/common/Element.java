package sjtu.ipads.wtune.testbed.common;

import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.schema.Column;

public interface Element {
  String collectionName();

  String elementName();

  <T> T unwrap(Class<T> cls);

  static Element ofColumn(Column column) {
    return new ColumnElement(column);
  }
}
