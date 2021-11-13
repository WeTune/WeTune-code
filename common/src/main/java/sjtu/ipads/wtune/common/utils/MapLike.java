package sjtu.ipads.wtune.common.utils;

import com.google.common.collect.Iterators;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface MapLike<K, V> extends Iterable<Pair<K, V>> {
  V get(Object key);

  int size();

  Set<K> keys();

  default Collection<V> values() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  default Iterator<Pair<K, V>> iterator() {
    return new MapLikeIterator<>(this);
  }

  default Map<K, V> asMap() {
    return new MapLikeWrapper<>(this);
  }
}

class MapLikeWrapper<K, V> implements Map<K, V> {
  private final MapLike<K, V> mapLike;

  MapLikeWrapper(MapLike<K, V> mapLike) {
    this.mapLike = mapLike;
  }

  @Override
  public int size() {
    return mapLike.size();
  }

  @Override
  public boolean isEmpty() {
    return mapLike.size() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    return mapLike.get(key) != null;
  }

  @Override
  public boolean containsValue(Object value) {
    return mapLike.values().contains(value);
  }

  @Override
  public V get(Object key) {
    return mapLike.get(key);
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public Set<K> keySet() {
    return mapLike.keys();
  }

  @NotNull
  @Override
  public Collection<V> values() {
    return mapLike.values();
  }

  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return new MapLikeEntrySet<>(mapLike);
  }
}

class MapLikeIterator<K, V> implements Iterator<Pair<K, V>> {
  private final MapLike<K, V> mapLike;
  private final Iterator<K> keyIterator;

  MapLikeIterator(MapLike<K, V> mapLike) {
    this.mapLike = mapLike;
    this.keyIterator = mapLike.keys().iterator();
  }

  @Override
  public boolean hasNext() {
    return keyIterator.hasNext();
  }

  @Override
  public Pair<K, V> next() {
    final K key = keyIterator.next();
    return Pair.of(key, mapLike.get(key));
  }
}

class MapLikeEntrySet<K, V> extends AbstractSet<Map.Entry<K, V>> implements Set<Map.Entry<K, V>> {
  private final MapLike<K, V> mapLike;

  MapLikeEntrySet(MapLike<K, V> mapLike) {
    this.mapLike = mapLike;
  }

  @Override
  public Iterator<Map.Entry<K, V>> iterator() {
    return Iterators.transform(mapLike.iterator(), it -> it);
  }

  @Override
  public int size() {
    return mapLike.size();
  }
}
