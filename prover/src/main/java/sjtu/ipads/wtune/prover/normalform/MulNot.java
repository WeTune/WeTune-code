package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.UExpr;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.MUL;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.NOT;

// not(x1) * not(x2) -> not(x1 + x2)
class MulNot extends TransformationBase {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (parent == null || parent.kind() != MUL) return point;
    final UExpr brother = UExpr.otherSide(parent, point);
    if (point.kind() != NOT || brother.kind() != NOT) return point;

    final UExpr x1 = point.child(0), x2 = brother.child(0);

    final UExpr newExpr = UExpr.not(UExpr.add(x1.copy(), x2.copy()));

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    ctx.trace("rw mul_not (%s) (%s)".formatted(x1, x2));

    return newExpr;
  }
}
