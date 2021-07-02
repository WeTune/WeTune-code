package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.ADD;

import java.util.List;
import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.UExpr;

class AddAssociativity extends TransformationBase {
  @Override
  public UExpr apply(UExpr point, Proof proof) {
    final UExpr parent = point.parent();
    if (parent == null
        || point.kind() != ADD
        || parent.kind() != ADD
        || parent.children().get(1) != point) return point;

    final List<UExpr> children = point.children();
    final UExpr x1 = children.get(0), x2 = children.get(1);
    final UExpr x3 = UExpr.otherSide(parent, point);

    final UExpr newExpr = UExpr.add(UExpr.add(x3.copy(), x1.copy()), x2.copy());

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    proof.append("rw add_assoc (%s) (%s) (%s)".formatted(x3, x1, x2));

    return newExpr;
  }
}
