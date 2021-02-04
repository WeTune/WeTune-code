package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.multiversion.Catalog2D;
import sjtu.ipads.wtune.sqlparser.ast.internal.FieldManagerImpl;

import java.util.Map;

public interface FieldManager extends Catalog2D<ASTNode, FieldKey, Object> {
  default boolean containsField(ASTNode owner, FieldKey<?> key) {
    return contains(owner, key);
  }

  @SuppressWarnings("unchecked")
  default <T> T getField(ASTNode owner, FieldKey<T> key) {
    return (T) get(owner, key);
  }

  @SuppressWarnings("unchecked")
  default <T> T unsetField(ASTNode owner, FieldKey<T> key) {
    return (T) remove(owner, key);
  }

  @SuppressWarnings("unchecked")
  default <T> T setField(ASTNode owner, FieldKey<T> key, T value) {
    return (T) put(owner, key, value);
  }

  default Map<FieldKey, Object> getFields(ASTNode owner) {
    return row(owner);
  }

  static FieldManager empty() {
    return FieldManagerImpl.build();
  }
}
