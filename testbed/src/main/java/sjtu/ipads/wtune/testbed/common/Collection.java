package sjtu.ipads.wtune.testbed.common;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.schema.Table;

public interface Collection {
  String collectionName();

  List<Element> elements();

  <T> T unwrap(Class<T> cls);

  static Collection ofTable(Table table) {
    return new TableCollection(table);
  }
}
