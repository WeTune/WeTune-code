package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.schema.Column;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

public class ColumnRef {
  private SQLNode node;
  private TableSource source;
  private Column refColumn;
  private SelectItem refItem;
  private boolean isDependent;

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

  public boolean isDependent() {
    return isDependent;
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

  public Column resolveAsColumn() {
    if (refColumn != null) return refColumn;
    if (refItem == null || refItem.expr() == null) return null;
    final ColumnRef columnRef = refItem.expr().get(RESOLVED_COLUMN_REF);
    return columnRef != null ? columnRef.resolveAsColumn() : null;
  }

  public void setDependent(boolean dependent) {
    isDependent = dependent;
  }
}
