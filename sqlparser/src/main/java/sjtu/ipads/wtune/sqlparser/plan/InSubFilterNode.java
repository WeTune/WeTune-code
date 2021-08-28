package sjtu.ipads.wtune.sqlparser.plan;

public interface InSubFilterNode extends FilterNode {
  RefBag lhsRefs();

  Expr lhsExpr();

  // RHS expression is the subquery expression.
  // e.g., a.id in (select x.id from x), RHS expr is (select x.id from x)
  // Since the subquery may depends on outer query's attributes (dependent subquery),
  // the RHS expression will be set during ref-binding.
  // The refs will be updated accordingly.
  // After that, this field is immutable.
  void setRhsExpr(Expr expr);

  @Override
  default OperatorType kind() {
    return OperatorType.IN_SUB_FILTER;
  }

  static InSubFilterNode mk(RefBag refs) {
    return InSubFilterNodeImpl.mk(refs);
  }
}
