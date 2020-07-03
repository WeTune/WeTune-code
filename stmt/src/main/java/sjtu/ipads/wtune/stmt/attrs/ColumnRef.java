package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.schema.Column;

import java.util.Objects;

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

  private ColumnRef nextLevel() {
    if (refItem == null || refItem.expr() == null) return null;
    return refItem.expr().get(RESOLVED_COLUMN_REF);
  }

  public ColumnRef resolveRootRef() {
    if (refColumn != null) return this;
    final ColumnRef columnRef = nextLevel();
    return columnRef != null ? columnRef.resolveRootRef() : this;
  }

  public Column resolveAsColumn() {
    final ColumnRef rootRef = resolveRootRef();
    if (rootRef != null) return rootRef.refColumn();
    else return null;
  }

  public boolean isFrom(TableSource source) {
    if (this.source.equals(source)) return true;
    final ColumnRef nextLevel = nextLevel();
    if (nextLevel != null) return nextLevel.isFrom(source);
    else return false;
  }

  public void setDependent(boolean dependent) {
    isDependent = dependent;
  }

  private static boolean equals0(ColumnRef ref, ColumnRef other) {
    return ref != null
        && other != null
        && Objects.equals(ref.source(), other.source())
        && Objects.equals(ref.refColumn(), other.refColumn())
        && Objects.equals(ref.refItem(), other.refItem());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ColumnRef columnRef = (ColumnRef) o;
    return equals0(resolveRootRef(), columnRef.resolveRootRef());
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, resolveRootRef());
  }

  @Override
  public String toString() {
    final ColumnRef rootRef = resolveRootRef();
    if (rootRef == this) return String.format("{%s | %s}", node, source);
    else return String.format("{%s < %s}", node, rootRef);
  }
}
