package sjtu.ipads.wtune.prover.uexpr;

import static java.util.Objects.requireNonNull;

abstract class BinaryExpr extends UExprBase {
  @Override
  public void subst(Var v1, Var v2) {
    requireNonNull(v1);
    requireNonNull(v2);
    child(0).subst(v1, v2);
    child(1).subst(v1, v2);
  }
}
