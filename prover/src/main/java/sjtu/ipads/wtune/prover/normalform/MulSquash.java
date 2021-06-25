package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.MUL;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.SQUASH;

import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.UExpr;

// squash(x1) * squash(x2) -> squash(x1 * x2)
final class MulSquash extends TransformationBase {
  @Override
  public UExpr apply(UExpr point, Proof proof) {
    final UExpr parent = point.parent();
    if (parent == null || parent.kind() != MUL) return point;
    final UExpr brother = UExpr.otherSide(parent, point);
    if (point.kind() != SQUASH || brother.kind() != SQUASH) return point;

    final UExpr x1 = point.child(0), x2 = brother.child(0);

    final UExpr newExpr = UExpr.squash(UExpr.mul(x1.copy(), x2.copy()));

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    proof.append("rw mul_squash (%s) (%s)".formatted(x1, x2));

    return newExpr;
  }
}
