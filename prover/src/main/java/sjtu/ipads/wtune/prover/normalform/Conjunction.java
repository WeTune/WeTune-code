package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.Var;

import java.util.List;

public interface Conjunction {
  List<Var> vars();

  List<UExpr> preds();

  List<UExpr> tables();

  Disjunction neg();

  Disjunction squash();

  UExpr toExpr();

  boolean isEmpty();

  boolean uses(Var v);

  void subst(Var v1, Var v2);

  Conjunction copy();

  static Conjunction mk(
      List<Var> sumVars,
      List<UExpr> tables,
      List<UExpr> predicates,
      Disjunction squash,
      Disjunction negation) {
    return ConjunctionImpl.make(sumVars, tables, predicates, squash, negation);
  }
}
