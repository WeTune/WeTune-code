package sjtu.ipads.wtune.prover;

import sjtu.ipads.wtune.prover.expr.Name;
import sjtu.ipads.wtune.prover.expr.Tuple;

public interface Congruence {
  boolean isCongruent(Tuple t1, Tuple t2);

  boolean isCongruent(Name n1, Name n2);

  static Congruence make() {
    return new CongruenceImpl();
  }
}
