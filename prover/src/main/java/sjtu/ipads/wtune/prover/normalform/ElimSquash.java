package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.NOT;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.SQUASH;

import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.UExpr;

// squash(...squash(x1)...) -> squash(...x1...)
// not(...squash(x1)...) -> not(...x1...)
final class ElimSquash extends TransformationBase {
  @Override
  public UExpr apply(UExpr point, Proof proof) {
    if (point.kind() != SQUASH) return point;

    UExpr parent = point.parent();
    while (parent != null) {
      if (parent.kind() == SQUASH || parent.kind() == NOT) break;
      parent = parent.parent();
    }
    if (parent == null) return point;

    UExpr.replaceChild(point.parent(), point, point.child(0).copy());

    // TODO: proof

    return point.parent();
  }
}
