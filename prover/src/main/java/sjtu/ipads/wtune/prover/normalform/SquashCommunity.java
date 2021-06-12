package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.UExpr;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.MUL;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.SQUASH;

// let x2 = squash(...)
// x1 * x2 -> x2 * x1 where x1 is not squash(..)
public class SquashCommunity implements Transformation {
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
