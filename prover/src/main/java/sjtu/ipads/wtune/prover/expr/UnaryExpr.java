package sjtu.ipads.wtune.prover.expr;

import static java.util.Objects.requireNonNull;

abstract class UnaryExpr extends UExprBase {

  @Override
  public void subst(Tuple v1, Tuple v2) {
    requireNonNull(v1);
    requireNonNull(v2);
    children[0].subst(v1, v2);
  }
}
