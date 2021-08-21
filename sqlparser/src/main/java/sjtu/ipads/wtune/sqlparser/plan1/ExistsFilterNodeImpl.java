package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.EXISTS_SUBQUERY_EXPR;

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
  public PlanNode copy(PlanContext ctx) {
    checkContextSet();

    final ExistsFilterNode copy = new ExistsFilterNodeImpl();
    copy.setContext(ctx);

    ctx.registerRefs(copy, refs());
    for (Ref ref : refs()) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public StringBuilder stringify0(StringBuilder builder) {
    builder.append("Exists{");
    stringifyRefs(builder);
    builder.append('}');
    stringifyChildren(builder);
    return builder;
  }
}
