package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;

// squash(squash(x)) -> squash(x)
class SquashSquash extends TransformationBase {
  @Override
  public UExpr apply(UExpr point, Proof proof) {
    final UExpr parent = point.parent();
    if (point.kind() != Kind.SQUASH || parent == null || parent.kind() != Kind.SQUASH) return point;

    final UExpr x = point.child(0);
    final UExpr newExpr = UExpr.squash(x.copy());

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    return newExpr;
  }
}
