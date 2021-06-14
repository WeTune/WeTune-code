package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.expr.SumExpr;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.SUM;
import static sjtu.ipads.wtune.prover.expr.UExpr.sum;

// sum(sum(x1)) -> sum(x1)
class SumSum extends TransformationBase {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (parent == null || point.kind() != SUM || parent.kind() != SUM) return point;

    final List<Tuple> tuples0 = ((SumExpr) parent).boundTuples();
    final List<Tuple> tuples1 = ((SumExpr) point).boundTuples();

    final UExpr newExpr = sum(listJoin(tuples0, tuples1), point.child(0).copy());

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    ctx.append("rw sum_sum (%s)".formatted(point.child(0)));
    return newExpr;
  }
}
