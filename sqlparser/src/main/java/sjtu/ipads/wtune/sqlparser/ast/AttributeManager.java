package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.ast.internal.AttributeManagerImpl;
import sjtu.ipads.wtune.sqlparser.ast.multiversion.Catalog2D;

import java.util.Map;

public interface AttributeManager extends Catalog2D<SQLNode, AttrKey, Object> {
  default boolean containsAttr(SQLNode owner, AttrKey<?> key) {
    return contains(owner, key);
  }

  @SuppressWarnings("unchecked")
  default <T> T getAttr(SQLNode owner, AttrKey<T> key) {
    return (T) get(owner, key);
  }

  @SuppressWarnings("unchecked")
  default <T> T unsetAttr(SQLNode owner, AttrKey<T> key) {
    return (T) remove(owner, key);
  }

  @SuppressWarnings("unchecked")
  default <T> T setAttr(SQLNode owner, AttrKey<T> key, T value) {
    return (T) put(owner, key, value);
  }

  default Map<AttrKey, Object> getAttrs(SQLNode owner) {
    return row(owner);
  }

  static AttributeManager empty() {
    return AttributeManagerImpl.build();
  }
}
