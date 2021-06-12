package sjtu.ipads.wtune.prover.expr;

import com.google.common.collect.Sets;

import java.util.Set;

abstract class BinaryExpr extends UExprBase {
  @Override
  public Set<Tuple> rootTuples() {
    return Sets.union(children[0].rootTuples(), children[1].rootTuples());
  }

  @Override
  public void replace(Tuple v1, Tuple v2) {
    for (UExpr child : children()) child.replace(v1, v2);
  }
}
