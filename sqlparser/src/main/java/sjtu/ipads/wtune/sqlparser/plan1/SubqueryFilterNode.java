package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface SubqueryFilterNode extends FilterNode {
  Expr lhsExpr();

  // RHS expression is the subquery expression.
  // e.g., a.id in (select x.id from x), RHS expr is (select x.id from x)
  // Since the subquery may depends on outer query's attributes (dependent subquery),
  // the RHS expression will be set during ref-binding.
  // The refs will be updated accordingly.
  // After that, this field is immutable.
  void setRhsExpr(Expr expr);

  @Override
  default OperatorType type() {
    return OperatorType.SubqueryFilter;
  }
}
