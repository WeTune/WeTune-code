package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

class LimitNodeImpl extends PlanNodeBase implements LimitNode {
  private final Expr limit, offset;

  LimitNodeImpl(Expr limit, Expr offset) {
    this.limit = limit;
    this.offset = offset;
  }

  static LimitNode mk(ASTNode limitExpr, ASTNode offsetExpr) {
    final Expr limit = limitExpr == null ? null : ExprImpl.mk(limitExpr);
    final Expr offset = offsetExpr == null ? null : ExprImpl.mk(offsetExpr);
    return new LimitNodeImpl(limit, offset);
  }

  @Override
  public Expr limit() {
    return limit;
  }

  @Override
  public Expr offset() {
    return offset;
  }

  @Override
  public ValueBag values() {
    return predecessors[0].values();
  }

  @Override
  public RefBag refs() {
    return RefBag.empty();
  }

  @Override
  protected PlanNode copy0(PlanContext ctx) {
    final LimitNode copy = new LimitNodeImpl(limit, offset);
    copy.setContext(ctx);
    return copy;
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    builder.append("Limit{");
    if (limit != null) builder.append(limit);
    if (offset != null) builder.append(',').append(offset);
    builder.append('}');
    stringifyChildren(builder);
    return builder;
  }
}
