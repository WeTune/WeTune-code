package sjtu.ipads.wtune.prover.normalform;

import java.util.List;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

public interface Conjunction {

  List<Tuple> vars();

  List<UExpr> predicates();

  List<UExpr> tables();

  Disjunction negation();

  Disjunction squash();

  UExpr toExpr();

  void subst(Tuple v1, Tuple v2);

  boolean uses(Tuple v);

  Conjunction copy();

  static Conjunction make(
      List<Tuple> sumTuples,
      List<UExpr> tables,
      List<UExpr> predicates,
      Disjunction squash,
      Disjunction negation) {
    return new ConjunctionImpl(sumTuples, tables, predicates, squash, negation);
  }
}
