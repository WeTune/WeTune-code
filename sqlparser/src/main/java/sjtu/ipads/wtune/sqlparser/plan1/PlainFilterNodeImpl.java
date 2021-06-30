package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

class PlainFilterNodeImpl extends PlanNodeBase implements PlainFilterNode {
  private final Expr predicate;

  PlainFilterNodeImpl(Expr predicate) {
    this.predicate = predicate;
  }

  static PlainFilterNode build(ASTNode predicate) {
    return new PlainFilterNodeImpl(ExprImpl.build(predicate));
  }

  @Override
  public Expr predicate() {
    return predicate;
  }

  @Override
  public ValueBag values() {
    return predecessors[0].values();
  }

  @Override
  public RefBag refs() {
    return predicate.refs();
  }

  @Override
  protected PlanNode copy0(PlanContext ctx) {
    checkContextSet();

    final PlainFilterNode copy = new PlainFilterNodeImpl(predicate);
    copy.setContext(ctx);

    ctx.registerRefs(copy, refs());
    for (Ref ref : refs()) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("Filter{").append(predicate);
    final RefBag refs = refs();
    if (!refs.isEmpty()) {
      builder.append(",refs=");
      if (context == null) builder.append(refs);
      else builder.append(context.deRef(refs));
    }
    builder.append('}');
    if (predecessors[0] != null) builder.append('(').append(predecessors[0]).append(')');
    return builder.toString();
  }
}
