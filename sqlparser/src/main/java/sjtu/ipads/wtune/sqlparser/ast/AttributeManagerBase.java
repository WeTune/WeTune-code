package sjtu.ipads.wtune.sqlparser.ast;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.multiversion.Snapshot;

public abstract class AttributeManagerBase<T> implements AttributeManager<T> {
  private static final Object REMOVED = new Object();
  private Mgr tail;

  public AttributeManagerBase() {}

  public AttributeManagerBase(boolean init) {
    if (init) tail = new Mgr(null);
  }

  @Override
  public T get(ASTNode owner) {
    return tail == null ? owner.get(fieldKey()) : tail.get(owner);
  }

  @Override
  public T set(ASTNode owner, T value) {
    return tail == null ? owner.set(fieldKey(), value) : tail.set(owner, value);
  }

  @Override
  public T unset(ASTNode owner) {
    return tail == null ? owner.unset(fieldKey()) : tail.unset(owner);
  }

  @Override
  public Map<ASTNode, T> attributes() {
    return tail != null ? tail.attributes() : Collections.emptyMap();
  }

  @Override
  public Snapshot derive() {
    final Snapshot snapshot = Snapshot.make();
    snapshot.put(key(), tail);

    tail = new Mgr(tail);

    return snapshot;
  }

  @Override
  public void rollback(Snapshot snapshot) {
    tail = (Mgr) snapshot.get(key());
  }

  protected abstract FieldKey<T> fieldKey();

  private class Mgr implements AttributeManager<T> {
    private final Mgr prev;
    private final Map<ASTNode, Object> map;

    private Mgr(Mgr prev) {
      this.prev = prev;
      this.map = new IdentityHashMap<>();
    }

    @Override
    public T get(ASTNode owner) {
      requireNonNull(owner);

      final Object value = map.get(owner);
      if (value == REMOVED) return null;
      if (value != null) return (T) value;

      return prev == null ? FieldKey.get0(owner, fieldKey()) : prev.get(owner);
    }

    @Override
    public T set(ASTNode owner, Object value) {
      requireNonNull(owner);
      if (value == null) return unset(owner);

      final T originalValue = get(owner);
      map.put(owner, value);
      return originalValue;
    }

    @Override
    public T unset(ASTNode owner) {
      final T originalValue = get(owner);
      map.put(owner, REMOVED);
      return originalValue;
    }

    @Override
    public Map<ASTNode, T> attributes() {
      if (prev == null) return new HashMap<>();

      final Map<ASTNode, T> attributes = prev.attributes();
      for (var pair : map.entrySet()) {
        if (pair.getValue() == REMOVED) attributes.remove(pair.getKey());
        else attributes.put(pair.getKey(), (T) pair.getValue());
      }

      return attributes;
    }

    @Override
    public Class<?> key() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Snapshot derive() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rollback(Snapshot snapshot) {
      throw new UnsupportedOperationException();
    }
  }
}
