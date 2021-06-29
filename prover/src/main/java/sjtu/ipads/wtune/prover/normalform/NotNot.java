package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;

// not(not(x)) -> squash(x)
class NotNot extends TransformationBase {
  @Override
  public UExpr apply(UExpr point, Proof proof) {
    final UExpr parent = point.parent();
    if (point.kind() != Kind.NOT || parent == null || parent.kind() != Kind.NOT) return point;

    final UExpr x = point.child(0);
    final UExpr newExpr = UExpr.squash(x.copy());

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    return newExpr;
  }
}
