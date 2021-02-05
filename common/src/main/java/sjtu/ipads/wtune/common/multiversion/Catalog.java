package sjtu.ipads.wtune.common.multiversion;

import java.util.Set;

public interface Catalog<K, V> extends MultiVersion {
  boolean contains(K k);

  V get(K k);

  V put(K k, V v);

  V remove(K k);

  Set<K> keys();
}
