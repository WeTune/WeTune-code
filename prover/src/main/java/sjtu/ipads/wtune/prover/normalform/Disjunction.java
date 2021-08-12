package sjtu.ipads.wtune.prover.normalform;

import java.util.Iterator;
import java.util.List;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.Var;

public interface Disjunction extends Iterable<Conjunction> {
  List<Conjunction> conjunctions();

  Disjunction copy();

  boolean uses(Var v);

  void subst(Var target, Var rep);

  UExpr toExpr();

  @Override
  default Iterator<Conjunction> iterator() {
    return conjunctions().iterator();
  }

  static Disjunction mk(List<Conjunction> cs) {
    return new DisjunctionImpl(cs);
  }
}
