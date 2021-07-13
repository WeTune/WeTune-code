package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.common.utils.FuncUtils.any;

import sjtu.ipads.wtune.prover.expr.SumExpr;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.expr.UExpr.Kind;

// Sum[t1](f(t1) * f'(t2)) => f'(t2) * Sum[t1](f(t1))
class SumMul extends TransformationBase {
  @Override
  public UExpr apply(UExpr point) {
    final UExpr parent = point.parent();
    if (parent == null || parent.kind() != Kind.MUL) return point;

    UExpr ancestor = parent;
    while (ancestor != null) {
      if (ancestor.kind() == Kind.SUM) break;
      if (ancestor.kind() != Kind.MUL) return point;
      ancestor = ancestor.parent();
    }
    if (ancestor == null) return point;

    final SumExpr sumExpr = (SumExpr) ancestor;
    if (any(sumExpr.boundedVars(), point::uses)) return point;

    UExpr.replaceChild(parent.parent(), parent, UExpr.otherSide(parent, point).copy());

    final UExpr newExpr = UExpr.mul(point.copy(), ancestor.copy());
    if (ancestor.parent() == null) return newExpr;

    UExpr.replaceChild(ancestor.parent(), ancestor, newExpr);
    return newExpr;
  }
}
