package sjtu.ipads.wtune.common.multiversion;

import java.util.*;

public class CatalogBase<K, V> extends MultiVersionBase<Map<K, Object>, Catalog<K, V>>
    implements Catalog<K, V> {
  // invariant: current == null => prev == null
  protected static final Object REMOVED = new Object();

  protected CatalogBase() {}

  protected CatalogBase(Map<K, Object> current, Catalog<K, V> prev) {
    super(current, prev);
  }

  @Override
  public boolean contains(K k) {
    if (current == null) return fallbackContains(k);
    else if (current.containsKey(k)) return current.get(k) != REMOVED;
    else if (prev == null) return fallbackContains(k);
    else return prev.contains(k);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V get(K k) {
    if (current == null) return fallbackGet(k);

    final Object t = current.get(k);
    if (t == REMOVED) return null;
    else if (t != null) return (V) t;
    else if (prev == null) return fallbackGet(k);
    else return prev.get(k);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V put(K k, V v) {
    if (current == null) return fallbackPut(k, v);

    final Object old = current.put(k, v);
    return old == REMOVED ? null : (V) old;
  }

  @Override
  @SuppressWarnings("unchecked")
  public V remove(K k) {
    if (current == null) return fallbackRemove(k);

    final Object old = current.put(k, REMOVED);
    return old == REMOVED ? null : (V) old;
  }

  @Override
  public Set<K> keys() {
    final Set<K> set;

    if (prev == null) set = new HashSet<>(fallbackKeys());
    else set = prev.keys();

    if (current != null)
      for (var pair : current.entrySet()) {
        if (pair.getValue() == REMOVED) set.remove(pair.getKey());
        else set.add(pair.getKey());
      }

    return set;
  }

  @Override
  protected Map<K, Object> makeCurrent() {
    return new IdentityHashMap<>();
  }

  @Override
  protected Catalog<K, V> makePrev(Map<K, Object> current, Catalog<K, V> prev) {
    return new CatalogBase<>(current, prev);
  }

  protected boolean fallbackContains(K k) {
    return false;
  }

  protected V fallbackGet(K k) {
    return null;
  }

  protected V fallbackPut(K k, V v) {
    return null;
  }

  protected V fallbackRemove(K k) {
    return null;
  }

  protected Set<K> fallbackKeys() {
    return Collections.emptySet();
  }
}
