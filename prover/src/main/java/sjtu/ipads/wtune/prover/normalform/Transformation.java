package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.UExpr;

public interface Transformation {
  UExpr apply(UExpr point, Proof proof);
}
