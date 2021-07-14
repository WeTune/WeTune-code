package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.uexpr.UExpr.Kind.MUL;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.Kind.SQUASH;

import sjtu.ipads.wtune.prover.uexpr.UExpr;

// let x2 = squash(...)
// x1 * x2 -> x2 * x1 where x1 is not squash(..)
final class SquashCommunity extends TransformationBase {
  @Override
  public UExpr apply(UExpr point) {
    if (point.kind() != MUL) return point;

    final UExpr x1 = point.child(0), x2 = point.child(1);
    if (x1.kind() == SQUASH || x2.kind() != SQUASH) return point;

    final UExpr newExpr = UExpr.mul(x2.copy(), x1.copy());

    final UExpr parent = point.parent();
    if (parent != null) UExpr.replaceChild(parent, point, newExpr);

    return newExpr;
  }
}
