package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.uexpr.UExpr.Kind.ADD;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.Kind.SUM;

import java.util.List;
import sjtu.ipads.wtune.prover.uexpr.SumExpr;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.Var;

// sum(x1 + x2) -> sum(x1) + sum(x2)
final class SumAdd extends TransformationBase {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (parent == null || point.kind() != ADD || parent.kind() != SUM) return point;

    final UExpr x1 = point.child(0), x2 = point.child(1);
    final List<Var> boundVar = ((SumExpr) parent).boundedVars();

    final UExpr newExpr = UExpr.add(UExpr.sum(boundVar, x1.copy()), UExpr.sum(boundVar, x2.copy()));

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    return newExpr;
  }
}
