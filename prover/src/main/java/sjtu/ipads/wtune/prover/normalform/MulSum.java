package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.SumExpr;
import sjtu.ipads.wtune.prover.expr.UExpr;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.MUL;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.SUM;

// x2 * sum(x1) -> sum(x1 * x2)
class MulSum extends TransformationBase {
  @Override
  public UExpr apply(UExpr point, Proof proof) {
    final UExpr parent = point.parent();
    if (parent == null || point.kind() != SUM || parent.kind() != MUL) return point;

    final UExpr x1 = point.child(0);
    final UExpr x2 = UExpr.otherSide(parent, point);

    final UExpr newExpr =
        UExpr.sum(((SumExpr) point).boundedVars(), UExpr.mul(x1.copy(), x2.copy()));

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    proof.append("rw mul_sum (%s) (%s)".formatted(x2, x1));

    return newExpr;
  }
}
