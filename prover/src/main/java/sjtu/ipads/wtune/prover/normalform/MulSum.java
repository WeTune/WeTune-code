package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.MUL;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.SUM;

import sjtu.ipads.wtune.prover.expr.SumExpr;
import sjtu.ipads.wtune.prover.expr.UExpr;

// x2 * sum(x1) -> sum(x1 * x2)
final class MulSum extends TransformationBase {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (parent == null || point.kind() != SUM || parent.kind() != MUL) return point;

    final UExpr x1 = point.child(0);
    final UExpr x2 = UExpr.otherSide(parent, point);

    final UExpr newExpr =
        UExpr.sum(((SumExpr) point).boundedVars(), UExpr.mul(x1.copy(), x2.copy()));

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    return newExpr;
  }
}
