package sjtu.ipads.wtune.sqlparser.ast.internal;

import com.google.common.collect.Table;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.FieldManager;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.multiversion.Catalog2D;
import sjtu.ipads.wtune.sqlparser.multiversion.Catalog2DBase;

import java.util.Map;

public class FieldManagerImpl extends Catalog2DBase<SQLNode, FieldKey, Object>
    implements FieldManager {

  protected FieldManagerImpl() {
    super();
  }

  protected FieldManagerImpl(
      Table<SQLNode, FieldKey, Object> current, Catalog2D<SQLNode, FieldKey, Object> prev) {
    this.current = current;
    this.prev = prev;
  }

  public static FieldManager build() {
    return new FieldManagerImpl();
  }

  @Override
  protected boolean fallbackContains(SQLNode row, FieldKey column) {
    return FieldKey.isPresent(row, column);
  }

  @Override
  protected Object fallbackGet(SQLNode row, FieldKey column) {
    return FieldKey.get0(row, column);
  }

  @Override
  protected Object fallbackPut(SQLNode row, FieldKey column, Object value) {
    return FieldKey.set0(row, column, value);
  }

  @Override
  protected Object fallbackRemove(SQLNode row, FieldKey column) {
    return FieldKey.unset0(row, column);
  }

  @Override
  protected Map<FieldKey, Object> fallbackRow(SQLNode row) {
    return row.directAttrs();
  }

  @Override
  protected Map<SQLNode, Object> fallbackColumn(FieldKey column) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Catalog2D<SQLNode, FieldKey, Object> makePrev(
      Table<SQLNode, FieldKey, Object> current, Catalog2D<SQLNode, FieldKey, Object> prev) {
    return new FieldManagerImpl(current, prev);
  }
}
