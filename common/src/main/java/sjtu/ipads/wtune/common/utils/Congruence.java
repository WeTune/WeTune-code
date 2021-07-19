package sjtu.ipads.wtune.common.utils;

import java.util.Set;

public interface Congruence<T> {
  Set<T> keys();

  Set<T> eqClassOf(T x);

  void putCongruent(T x, T y);

  boolean isCongruent(T x, T y);

  Set<T> makeClass(T x);

  static <T> Congruence<T> mk() {
    return new CongruenceImpl<>();
  }
}
