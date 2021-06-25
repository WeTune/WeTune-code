package sjtu.ipads.wtune.prover.normalform;

import sjtu.ipads.wtune.prover.Proof;
import sjtu.ipads.wtune.prover.expr.SumExpr;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

import java.util.List;

import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.ADD;
import static sjtu.ipads.wtune.prover.expr.UExpr.Kind.SUM;

// sum(x1 + x2) -> sum(x1) + sum(x2)
final class SumAdd extends TransformationBase {
  @Override
  public UExpr apply(UExpr point, Proof proof) {
    final UExpr parent = point.parent();
    if (parent == null || point.kind() != ADD || parent.kind() != SUM) return point;

    final UExpr x1 = point.child(0), x2 = point.child(1);
    final List<Tuple> boundTuple = ((SumExpr) parent).boundedVars();

    final UExpr newExpr =
        UExpr.add(UExpr.sum(boundTuple, x1.copy()), UExpr.sum(boundTuple, x2.copy()));

    final UExpr grandpa = parent.parent();
    if (grandpa != null) UExpr.replaceChild(grandpa, parent, newExpr);

    proof.append("rw sum_add (%s) (%s)".formatted(x1, x2));

    return newExpr;
  }
}
