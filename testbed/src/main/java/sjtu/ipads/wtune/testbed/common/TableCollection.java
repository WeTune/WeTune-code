package sjtu.ipads.wtune.testbed.common;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.schema.Table;

class TableCollection implements Collection {
  private final Table table;
  private final List<Element> elements;

  TableCollection(Table table) {
    this.table = table;
    this.elements = listMap(Element::ofColumn, table.columns());
  }

  @Override
  public String collectionName() {
    return table.name();
  }

  @Override
  public List<Element> elements() {
    return elements;
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (cls == Table.class) return (T) table;
    else return null;
  }
}
