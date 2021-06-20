package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

class LimitNodeImpl extends PlanNodeBase implements LimitNode {
  private final Expr limit, offset;

  LimitNodeImpl(Expr limit, Expr offset) {
    this.limit = limit;
    this.offset = offset;
  }

  static LimitNode build(ASTNode limitExpr, ASTNode offsetExpr) {
    final Expr limit = limitExpr == null ? null : ExprImpl.build(limitExpr);
    final Expr offset = offsetExpr == null ? null : ExprImpl.build(offsetExpr);
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
    return ValueBag.empty();
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
  public String toString() {
    final StringBuilder builder = new StringBuilder("Limit{");
    if (limit != null) builder.append(limit);
    if (offset != null) builder.append(offset);
    builder.append('}');
    if (predecessors[0] != null) builder.append('(').append(predecessors[0]).append(')');
    return builder.toString();
  }
}
