package sjtu.ipads.wtune.sqlparser.plan1;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.listConcat;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;

class SubqueryFilterNodeImpl extends PlanNodeBase implements SubqueryFilterNode {
  private final Expr lhsExpr;
  private Expr rhsExpr, predicate;
  private RefBag refs;

  SubqueryFilterNodeImpl(Expr lhsExpr) {
    this.lhsExpr = lhsExpr;
    this.refs = lhsExpr.refs();
  }

  static SubqueryFilterNode build(ASTNode node) {
    return new SubqueryFilterNodeImpl(ExprImpl.build(node));
  }

  @Override
  public Expr predicate() {
    if (predicate == null)
      throw new IllegalStateException(
          "SubqueryFilter.predicate() cannot be called before setRhsExpr() called");

    return predicate;
  }

  @Override
  public ValueBag values() {
    return predecessors[0].values();
  }

  @Override
  public RefBag refs() {
    return refs;
  }

  @Override
  public Expr lhsExpr() {
    return lhsExpr;
  }

  @Override
  public void setRhsExpr(Expr rhsExpr) {
    if (this.rhsExpr != null)
      throw new IllegalStateException("RHS expression of subquery filter is immutable once set");

    this.rhsExpr = requireNonNull(rhsExpr);

    // update refs
    if (!rhsExpr.refs().isEmpty())
      this.refs = new RefBagImpl(listConcat(lhsExpr.refs(), rhsExpr.refs()));

    // update predicate
    final ASTNode predicateExpr = ASTNode.expr(ExprKind.BINARY);
    predicateExpr.set(BINARY_OP, BinaryOp.IN_SUBQUERY);
    predicateExpr.set(BINARY_LEFT, lhsExpr.template().deepCopy());
    predicateExpr.set(BINARY_RIGHT, rhsExpr.template().deepCopy());
    this.predicate = new ExprImpl(refs, predicateExpr);
  }

  @Override
  protected PlanNode copy0(PlanContext ctx) {
    checkContextSet();

    final SubqueryFilterNode copy = new SubqueryFilterNodeImpl(lhsExpr);
    copy.setContext(ctx);

    ctx.registerRefs(copy, refs());
    for (Ref ref : refs()) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("SubFilter{").append(lhsExpr);
    if (!refs.isEmpty()) {
      builder.append(",refs=");
      if (context == null) builder.append(refs);
      else builder.append(listMap(context::deRef, refs));
    }
    builder.append('}');
    if (predecessors[0] != null && predecessors[1] != null)
      builder.append('(').append(predecessors[0]).append(',').append(predecessors[1]).append(')');
    return builder.toString();
  }
}
