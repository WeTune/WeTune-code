package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.UExpr;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.ADD;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.MUL;

// (x1 + x2) * x3 -> x1 * x3 + x2 * x3
// x3 * (x1 + x2) -> x1 * x3 + x2 * x3
class Distribution extends TransformationBase {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (parent == null || point.kind() != ADD || parent.kind() != MUL) return point;

    final UExpr x1 = point.child(0), x2 = point.child(1);
    final UExpr x3 = UExpr.otherSide(parent, point);

    final UExpr mul0 = UExpr.mul(x1.copy(), x3.copy());
    final UExpr mul1 = UExpr.mul(x2.copy(), x3.copy());

    final UExpr newExpr = UExpr.add(mul0, mul1);

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    if (point == parent.child(0)) ctx.append("rw mul_distrib_right %s %s %s".formatted(x1, x2, x3));
    else ctx.append("rw mul_distrib_left (%s) (%s) (%s)".formatted(x3, x1, x2));

    return newExpr;
  }
}
