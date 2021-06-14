package sjtu.ipads.wtune.prover.expr;

import com.google.common.collect.Sets;

import java.util.Set;

import static java.util.Objects.requireNonNull;

abstract class BinaryExpr extends UExprBase {
  @Override
  public Set<Tuple> rootTuples() {
    return Sets.union(children[0].rootTuples(), children[1].rootTuples());
  }

  @Override
  public void subst(Tuple v1, Tuple v2) {
    requireNonNull(v1);
    requireNonNull(v2);
    child(0).subst(v1, v2);
    child(1).subst(v1, v2);
  }
}
