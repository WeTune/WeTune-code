package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.multiversion.Catalog2D;
import sjtu.ipads.wtune.sqlparser.ast.internal.FieldManagerImpl;

import java.util.Map;

public interface FieldManager extends Catalog2D<SQLNode, FieldKey, Object> {
  default boolean containsField(SQLNode owner, FieldKey<?> key) {
    return contains(owner, key);
  }

  @SuppressWarnings("unchecked")
  default <T> T getField(SQLNode owner, FieldKey<T> key) {
    return (T) get(owner, key);
  }

  @SuppressWarnings("unchecked")
  default <T> T unsetField(SQLNode owner, FieldKey<T> key) {
    return (T) remove(owner, key);
  }

  @SuppressWarnings("unchecked")
  default <T> T setField(SQLNode owner, FieldKey<T> key, T value) {
    return (T) put(owner, key, value);
  }

  default Map<FieldKey, Object> getFields(SQLNode owner) {
    return row(owner);
  }

  static FieldManager empty() {
    return FieldManagerImpl.build();
  }
}
