package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.schema.Column;

public class ColumnRef {
  private SQLNode node;
  private TableSource source;
  private Column refColumn;
  private SelectItem refItem;

  public SQLNode node() {
    return node;
  }

  public TableSource source() {
    return source;
  }

  public Column refColumn() {
    return refColumn;
  }

  public SelectItem refItem() {
    return refItem;
  }

  public ColumnRef setNode(SQLNode node) {
    this.node = node;
    return this;
  }

  public void setSource(TableSource source) {
    this.source = source;
  }

  public void setRefColumn(Column refColumn) {
    this.refColumn = refColumn;
  }

  public ColumnRef setRefItem(SelectItem refItem) {
    this.refItem = refItem;
    return this;
  }
}
