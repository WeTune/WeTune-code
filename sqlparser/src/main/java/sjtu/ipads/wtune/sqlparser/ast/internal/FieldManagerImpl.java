package sjtu.ipads.wtune.sqlparser.ast.internal;

import static com.google.common.base.Equivalence.identity;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Equivalence;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.FieldManager;

public class FieldManagerImpl implements FieldManager {
  private final FieldManager prev;
  private final Table<Equivalence.Wrapper<ASTNode>, FieldKey, Object> table;

  private static final Object REMOVED = new Object();

  private FieldManagerImpl(FieldManager prev) {
    this.prev = prev;
    this.table = HashBasedTable.create();
  }

  public static FieldManager build(FieldManager prev) {
    return new FieldManagerImpl(prev);
  }

  @Override
  public boolean containsField(ASTNode owner, FieldKey<?> key) {
    requireNonNull(owner);

    final Object value = table.get(identity().wrap(owner), key);
    if (value == REMOVED) return false;

    return prev == null ? owner.directAttrs().containsKey(key) : prev.containsField(owner, key);
  }

  @Override
  public <T> T getField(ASTNode owner, FieldKey<T> key) {
    requireNonNull(owner);

    final Object value = table.get(identity().wrap(owner), key);
    if (value == REMOVED) return null;
    if (value != null) return (T) value;

    return prev == null ? (T) owner.directAttrs().get(key) : prev.getField(owner, key);
  }

  @Override
  public <T> T unsetField(ASTNode owner, FieldKey<T> key) {
    requireNonNull(owner);

    final T originalValue = getField(owner, key);
    table.put(identity().wrap(owner), key, REMOVED);
    return originalValue;
  }

  @Override
  public <T> T setField(ASTNode owner, FieldKey<T> key, T value) {
    requireNonNull(owner);
    if (value == null) return unsetField(owner, key);

    final T originalValue = getField(owner, key);
    table.put(identity().wrap(owner), key, value);
    return originalValue;
  }

  @Override
  public Map<FieldKey, Object> getFields(ASTNode owner) {
    if (prev == null) return new HashMap<>(owner.directAttrs());

    final Map<FieldKey, Object> values = prev.getFields(owner);
    final Map<FieldKey, Object> newValues = table.row(identity().wrap(owner));
    for (Entry<FieldKey, Object> pair : newValues.entrySet()) {
      if (pair.getValue() == REMOVED) values.remove(pair.getKey());
      else values.put(pair.getKey(), pair.getValue());
    }

    return values;
  }
}
