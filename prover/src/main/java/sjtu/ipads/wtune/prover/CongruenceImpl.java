package sjtu.ipads.wtune.prover;

import sjtu.ipads.wtune.prover.expr.Name;
import sjtu.ipads.wtune.prover.expr.Tuple;

public class CongruenceImpl implements Congruence {
  CongruenceImpl() {}

  @Override
  public boolean isCongruent(Tuple t1, Tuple t2) {
    return t1.equals(t2);
  }

  @Override
  public boolean isCongruent(Name n1, Name n2) {
    return n1.equals(n2);
  }
}
