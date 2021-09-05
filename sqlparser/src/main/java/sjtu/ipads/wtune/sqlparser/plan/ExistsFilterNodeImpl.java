package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.EXISTS_SUBQUERY_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.QUERY_EXPR_QUERY;

class ExistsFilterNodeImpl extends PlanNodeBase implements ExistsFilterNode {
  private Expr rhsExpr, predicate;
  private RefBag refs;

  ExistsFilterNodeImpl() {
    refs = RefBag.empty();
  }

  @Override
  public void setRhsExpr(Expr expr) {
    if (this.rhsExpr != null)
      throw new IllegalStateException("RHS expression of subquery filter is immutable once set");

    this.rhsExpr = requireNonNull(expr);

    // update refs
    refs = expr.refs();

    // update predicate
    final ASTNode predicateExpr = ASTNode.expr(ExprKind.EXISTS);
    final ASTNode queryExpr = ASTNode.expr(ExprKind.QUERY_EXPR);
    queryExpr.set(QUERY_EXPR_QUERY, expr.template().deepCopy());
    predicateExpr.set(EXISTS_SUBQUERY_EXPR, queryExpr);

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
  public RefBag rhsRefs() {
    return rhsExpr == null ? null : rhsExpr.refs();
  }

  @Override
  public PlanNode copy(PlanContext ctx) {
    checkContextSet();

    final ExistsFilterNodeImpl copy = new ExistsFilterNodeImpl();
    copy.refs = RefBag.mk(refs);
    if (rhsExpr != null) {
      copy.rhsExpr = rhsExpr.copy();
      copy.predicate = predicate.copy();
    }

    copy.setContext(ctx);

    ctx.registerRefs(copy, refs());
    for (Ref ref : refs()) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public StringBuilder stringify0(StringBuilder builder, boolean compact) {
    builder.append("Exists{");
    stringifyRefs(builder, compact);
    builder.append('}');
    stringifyChildren(builder, compact);
    return builder;
  }
}
