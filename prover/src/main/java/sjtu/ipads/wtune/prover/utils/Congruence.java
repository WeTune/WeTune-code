package sjtu.ipads.wtune.prover.utils;

import java.util.Set;

public interface Congruence<T> {

  Set<T> getClass(T x);

  void putCongruent(T x, T y);

  boolean isCongruent(T x, T y);

  Set<T> makeClass(T x);

  static <T> Congruence<T> make() {
    return new CongruenceImpl<>();
  }
}
