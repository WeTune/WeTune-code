package sjtu.ipads.wtune.common.multiversion;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface Catalog2D<R, C, V> extends Catalog<Pair<R, C>, V> {
  @Override
  default boolean contains(Pair<R, C> pair) {
    return contains(pair.getKey(), pair.getValue());
  }

  @Override
  default V get(Pair<R, C> pair) {
    return get(pair.getKey(), pair.getValue());
  }

  @Override
  default V put(Pair<R, C> pair, V v) {
    return put(pair.getKey(), pair.getValue(), v);
  }

  @Override
  default V remove(Pair<R, C> pair) {
    return remove(pair.getKey(), pair.getValue());
  }

  boolean contains(R row, C column);

  V get(R row, C column);

  V put(R row, C column, V value);

  V remove(R row, C column);

  Map<C, V> row(R row);

  Map<R, V> column(C column);
}
