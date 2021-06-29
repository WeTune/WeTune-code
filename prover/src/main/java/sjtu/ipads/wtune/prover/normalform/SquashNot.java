package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;

// squash(not(x)) -> not(x)
class SquashNot extends TransformationBase {
  @Override
  public UExpr apply(UExpr point, Proof proof) {
    final UExpr parent = point.parent();
    if (point.kind() != Kind.NOT || parent == null || parent.kind() != Kind.SQUASH) return point;

    final UExpr x = point.child(0);
    final UExpr newExpr = UExpr.not(x.copy());

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    return newExpr;
  }
}
