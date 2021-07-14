package sjtu.ipads.wtune.prover.utils;

import java.util.List;
import java.util.Set;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.Var;

public interface Congruence<T> {
  Set<T> keys();

  Set<T> eqClassOf(T x);

  void putCongruent(T x, T y);

  boolean isCongruent(T x, T y);

  Set<T> makeClass(T x);

  static <T> Congruence<T> mk() {
    return new CongruenceImpl<>();
  }

  static Congruence<Var> mk(List<UExpr> predicates) {
    return TupleCongruence.make(predicates);
  }
}
