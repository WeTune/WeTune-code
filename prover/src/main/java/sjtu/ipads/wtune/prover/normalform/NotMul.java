package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.UExpr.Kind;

// not(x1 * x2) -> not(x1) + not(x2)
class NotMul extends TransformationBase {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (point.kind() != Kind.MUL || parent == null || parent.kind() != Kind.NOT) return point;

    final UExpr x0 = point.child(0), x1 = point.child(1);
    final UExpr newExpr = UExpr.add(UExpr.not(x0.copy()), UExpr.not(x1.copy()));

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    return newExpr;
  }
}
