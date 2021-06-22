package sjtu.ipads.wtune.prover.normalform;

import java.util.List;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

public interface Disjunction {
  List<Conjunction> conjunctions();

  Disjunction copy();

  void subst(Tuple target, Tuple rep);

  UExpr toExpr();
}
