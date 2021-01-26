package sjtu.ipads.wtune.sqlparser.ast.internal;

import com.google.common.collect.Table;
import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.ast.AttributeManager;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.multiversion.Catalog2D;
import sjtu.ipads.wtune.sqlparser.ast.multiversion.Catalog2DBase;

import java.util.Map;

public class AttributeManagerImpl extends Catalog2DBase<SQLNode, AttrKey, Object>
    implements AttributeManager {

  protected AttributeManagerImpl() {
    super();
  }

  protected AttributeManagerImpl(
      Table<SQLNode, AttrKey, Object> current, Catalog2D<SQLNode, AttrKey, Object> prev) {
    this.current = current;
    this.prev = prev;
  }

  public static AttributeManager build() {
    return new AttributeManagerImpl();
  }

  @Override
  protected boolean fallbackContains(SQLNode row, AttrKey column) {
    return AttrKey.isPresent(row, column);
  }

  @Override
  protected Object fallbackGet(SQLNode row, AttrKey column) {
    return AttrKey.get(row, column);
  }

  @Override
  protected Object fallbackPut(SQLNode row, AttrKey column, Object value) {
    return AttrKey.set(row, column, value);
  }

  @Override
  protected Object fallbackRemove(SQLNode row, AttrKey column) {
    return AttrKey.unset(row, column);
  }

  @Override
  protected Map<AttrKey, Object> fallbackRow(SQLNode row) {
    return row.directAttrs();
  }

  @Override
  protected Map<SQLNode, Object> fallbackColumn(AttrKey column) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Catalog2D<SQLNode, AttrKey, Object> makePrev(
      Table<SQLNode, AttrKey, Object> current, Catalog2D<SQLNode, AttrKey, Object> prev) {
    return new AttributeManagerImpl(current, prev);
  }
}
