package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;

// squash(...squash(x1)...) -> squash(...x1...)
final class ElimSquash extends TransformationBase {
  @Override
  public UExpr apply(UExpr point, Proof proof) {
    if (point.kind() != Kind.SQUASH) return point;

    UExpr parent = point.parent();
    while (parent != null) {
      if (parent.kind() == Kind.SQUASH) break;
      parent = parent.parent();
    }
    if (parent == null) return point;

    UExpr.replaceChild(point.parent(), point, point.child(0).copy());

    // TODO: proof

    return point.parent();
  }
}
