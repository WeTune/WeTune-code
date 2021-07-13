package sjtu.ipads.wtune.sqlparser.plan1;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.EXISTS_SUBQUERY_EXPR;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;

class ExistsFilterNodeImpl extends PlanNodeBase implements ExistsFilterNode {
  private Expr expr, predicate;
  private RefBag refs;

  ExistsFilterNodeImpl() {
    refs = RefBag.empty();
  }

  @Override
  public void setExpr(Expr expr) {
    if (this.expr != null)
      throw new IllegalStateException("RHS expression of subquery filter is immutable once set");

    this.expr = requireNonNull(expr);

    // update refs
    refs = expr.refs();

    // update predicate
    final ASTNode predicateExpr = ASTNode.expr(ExprKind.EXISTS);
    predicateExpr.set(EXISTS_SUBQUERY_EXPR, expr.template().deepCopy());

    this.predicate = new ExprImpl(refs, predicateExpr);
  }

  @Override
  public Expr predicate() {
    if (predicate == null)
      throw new IllegalStateException(
          "ExistsFilter.predicate() cannot be called before setExpr() called");

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
  protected PlanNode copy0(PlanContext ctx) {
    checkContextSet();

    final ExistsFilterNode copy = new ExistsFilterNodeImpl();
    copy.setContext(ctx);

    ctx.registerRefs(copy, refs());
    for (Ref ref : refs()) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("Exists{");
    if (!refs.isEmpty()) {
      builder.append(",refs=");
      if (context == null) builder.append(refs);
      else builder.append(context.deRef(refs));
    }
    builder.append('}');
    if (predecessors[0] != null && predecessors[1] != null)
      builder.append('(').append(predecessors[0]).append(',').append(predecessors[1]).append(')');
    return builder.toString();
  }
}
