package sjtu.ipads.wtune.sqlparser.ast.internal;

import com.google.common.base.Equivalence;
import com.google.common.collect.Table;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.multiversion.Catalog2D;
import sjtu.ipads.wtune.common.multiversion.Catalog2DBase;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.FieldManager;

import java.util.Map;

public class FieldManagerImpl extends Catalog2DBase<ASTNode, FieldKey, Object>
    implements FieldManager {

  protected FieldManagerImpl() {
    super();
  }

  protected FieldManagerImpl(
      Table<Equivalence.Wrapper<ASTNode>, Equivalence.Wrapper<FieldKey>, Object> current,
      Catalog2D<ASTNode, FieldKey, Object> prev) {
    this.current = current;
    this.prev = prev;
  }

  public static FieldManager build() {
    return new FieldManagerImpl();
  }

  @Override
  protected boolean fallbackContains(ASTNode row, FieldKey column) {
    return FieldKey.isPresent(row, column);
  }

  @Override
  protected Object fallbackGet(ASTNode row, FieldKey column) {
    return FieldKey.get0(row, column);
  }

  @Override
  protected Object fallbackPut(ASTNode row, FieldKey column, Object value) {
    return FieldKey.set0(row, column, value);
  }

  @Override
  protected Object fallbackRemove(ASTNode row, FieldKey column) {
    return FieldKey.unset0(row, column);
  }

  @Override
  protected Map<FieldKey, Object> fallbackRow(ASTNode row) {
    return row.directAttrs();
  }

  @Override
  protected Map<ASTNode, Object> fallbackColumn(FieldKey column) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Catalog2D<ASTNode, FieldKey, Object> makePrev(
      Table<Equivalence.Wrapper<ASTNode>, Equivalence.Wrapper<FieldKey>, Object> current,
      Catalog2D<ASTNode, FieldKey, Object> prev) {
    return new FieldManagerImpl(current, prev);
  }
}
