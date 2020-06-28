package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.schema.Table;

public class TableSource {
  public SQLNode tableSource;
  public String name;
  public Table table;

  public String name() {
    return name;
  }

  public SQLNode tableSource() {
    return tableSource;
  }

  public Table table() {
    return table;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setTableSource(SQLNode tableSource) {
    this.tableSource = tableSource;
  }

  public void setTable(Table table) {
    this.table = table;
  }
}
