package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.UExpr;

import java.util.List;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.MUL;

// x3 * (x1 * x2) -> x3 * x1 * x2
public class Associativity implements Transformation {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (parent == null
        || point.kind() != MUL
        || parent.kind() != MUL
        || parent.children().get(1) != point) return point;

    final List<UExpr> children = point.children();
    final UExpr x1 = children.get(0), x2 = children.get(1);
    final UExpr x3 = UExpr.otherSide(parent, point);

    final UExpr newExpr = UExpr.mul(UExpr.mul(x3.copy(), x1.copy()), x2.copy());

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    return newExpr;
  }
}
