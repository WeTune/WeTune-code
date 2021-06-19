package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

import java.util.List;

public interface Conjunction {
  List<Tuple> boundedVars();

  List<UExpr> predicates();

  List<UExpr> tables();

  UExpr negation();

  UExpr squash();

  UExpr toExpr();

  void subst(Tuple v1, Tuple v2);

  Conjunction copy();
}
