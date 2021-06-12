package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.UExpr;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.MUL;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.SQUASH;

// squash(x1) * squash(x2) -> squash(x1 * x2)
public class MulSquash implements Transformation {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (parent == null || parent.kind() != MUL) return point;
    final UExpr brother = UExpr.otherSide(parent, point);
    if (point.kind() != SQUASH || brother.kind() != SQUASH) return point;

    final UExpr x1 = point.child(0), x2 = brother.child(0);

    final UExpr newExpr = UExpr.squash(UExpr.mul(x1.copy(), x2.copy()));

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    return newExpr;
  }
}
