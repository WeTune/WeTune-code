package sjtu.ipads.wtune.prover.normalform;

import java.util.List;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

public interface Conjunction {
  List<Tuple> boundedVars();

  List<UExpr> predicates();

  List<UExpr> tables();

  Disjunction negation();

  Disjunction squash();

  UExpr toExpr();

  void subst(Tuple v1, Tuple v2);

  Conjunction copy();
}
