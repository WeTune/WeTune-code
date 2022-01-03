package sjtu.ipads.wtune.sql.ast;

import java.util.Map;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sql.ast.internal.FieldManagerImpl;

public interface FieldManager {
  boolean containsField(ASTNode owner, FieldKey<?> key);

  <T> T getField(ASTNode owner, FieldKey<T> key);

  <T> T unsetField(ASTNode owner, FieldKey<T> key);

  <T> T setField(ASTNode owner, FieldKey<T> key, T value);

  Map<FieldKey, Object> getFields(ASTNode owner);

  static FieldManager make(FieldManager base) {
    return FieldManagerImpl.build(base);
  }
}
