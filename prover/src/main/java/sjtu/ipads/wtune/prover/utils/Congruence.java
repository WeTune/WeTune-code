package sjtu.ipads.wtune.prover.utils;

import java.util.List;
import java.util.Set;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

public interface Congruence<T> {

  Set<T> getClass(T x);

  void putCongruent(T x, T y);

  boolean isCongruent(T x, T y);

  Set<T> makeClass(T x);

  static <T> Congruence<T> make() {
    return new CongruenceImpl<>();
  }

  static Congruence<Tuple> make(List<UExpr> predicates) {
    return TupleCongruence.make(predicates);
  }
}
