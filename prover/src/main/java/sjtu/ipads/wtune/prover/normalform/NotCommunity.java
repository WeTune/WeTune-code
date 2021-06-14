package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.UExpr;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.MUL;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.NOT;

// let x2 = not(...)
// x1 * x2 -> x2 * x1 where x1 is not squash(..)
class NotCommunity extends TransformationBase {
  @Override
  public UExpr apply(UExpr point) {
    if (point.kind() != MUL) return point;

    final UExpr x1 = point.child(0), x2 = point.child(1);
    if (x1.kind() == NOT || x2.kind() != NOT) return point;

    final UExpr newExpr = UExpr.mul(x2.copy(), x1.copy());

    final UExpr parent = point.parent();
    if (parent != null) UExpr.replaceChild(parent, point, newExpr);

    ctx.trace("rw mul_comm (%s) (%s)".formatted(x1, x2));

    return newExpr;
  }
}
