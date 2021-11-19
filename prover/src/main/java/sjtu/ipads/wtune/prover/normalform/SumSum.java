package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.common.utils.ListSupport.join;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.Kind.SUM;
import static sjtu.ipads.wtune.prover.uexpr.UExpr.sum;

import java.util.List;
import sjtu.ipads.wtune.prover.uexpr.SumExpr;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.Var;

// sum(sum(x1)) -> sum(x1)
final class SumSum extends TransformationBase {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (parent == null || point.kind() != SUM || parent.kind() != SUM) return point;

    final List<Var> tuples0 = ((SumExpr) parent).boundedVars();
    final List<Var> tuples1 = ((SumExpr) point).boundedVars();

    final UExpr newExpr = sum(join(tuples0, tuples1), point.child(0).copy());

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    return newExpr;
  }
}
