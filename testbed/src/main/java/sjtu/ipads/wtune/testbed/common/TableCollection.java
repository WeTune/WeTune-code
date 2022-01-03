package sjtu.ipads.wtune.testbed.common;

import java.util.List;
import java.util.function.Function;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sql.schema.Column;
import sjtu.ipads.wtune.sql.schema.Table;

class TableCollection implements Collection {
  private final Table table;
  private final List<Element> elements;

  TableCollection(Table table) {
    this.table = table;
    this.elements = ListSupport.map((Iterable<Column>) table.columns(), (Function<? super Column, ? extends Element>) Element::ofColumn);
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
