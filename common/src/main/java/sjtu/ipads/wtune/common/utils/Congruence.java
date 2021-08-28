package sjtu.ipads.wtune.common.utils;

import java.util.Set;

public interface Congruence<K, T> {
  Set<K> keys();

  Set<T> eqClassOf(T x);

  Set<T> eqClassAt(K k);

  void putCongruent(T x, T y);

  boolean isCongruent(T x, T y);

  Set<T> mkEqClass(T x);
}
