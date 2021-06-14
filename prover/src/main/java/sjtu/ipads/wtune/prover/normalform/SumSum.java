package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.UExpr;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.SUM;

// sum(sum(x1)) -> sum(x1)
class SumSum extends TransformationBase {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (parent == null || point.kind() != SUM || parent.kind() != SUM) return point;

    final UExpr newExpr = point.copy();

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    ctx.trace("rw sum_sum (%s)".formatted(point.child(0)));
    return newExpr;
  }
}
