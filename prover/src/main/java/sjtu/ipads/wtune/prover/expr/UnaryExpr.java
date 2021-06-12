package sjtu.ipads.wtune.prover.expr;

import java.util.Set;

abstract class UnaryExpr extends UExprBase {
  @Override
  public Set<Tuple> rootTuples() {
    return children[0].rootTuples();
  }

  @Override
  public void replace(Tuple v1, Tuple v2) {
    children[0].replace(v1, v2);
  }
}
