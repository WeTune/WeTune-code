package sjtu.ipads.wtune.prover.normalform;

import java.util.Iterator;
import java.util.List;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

public interface Disjunction extends Iterable<Conjunction> {
  List<Conjunction> conjunctions();

  Disjunction copy();

  void subst(Tuple target, Tuple rep);

  UExpr toExpr();

  boolean uses(Tuple v);

  @Override
  default Iterator<Conjunction> iterator() {
    return conjunctions().iterator();
  }

  static Disjunction mk(List<Conjunction> cs) {
    return new DisjunctionImpl(cs);
  }
}
