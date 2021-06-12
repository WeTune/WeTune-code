package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.UExpr;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.ADD;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.SUM;

// sum(x1 + x2) -> sum(x1) + sum(x2)
public class SumAdd implements Transformation {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (parent == null || point.kind() != ADD || parent.kind() != SUM) return point;

    final UExpr x1 = point.child(0), x2 = point.child(1);

    final UExpr newExpr = UExpr.add(UExpr.sum(x1.copy()), UExpr.sum(x2.copy()));

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    return newExpr;
  }
}
