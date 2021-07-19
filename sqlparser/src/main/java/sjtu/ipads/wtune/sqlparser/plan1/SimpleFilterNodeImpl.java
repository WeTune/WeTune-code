package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

class SimpleFilterNodeImpl extends PlanNodeBase implements SimpleFilterNode {
  private final Expr predicate;

  SimpleFilterNodeImpl(Expr predicate) {
    this.predicate = predicate;
  }

  static SimpleFilterNode mk(ASTNode predicate) {
    return new SimpleFilterNodeImpl(ExprImpl.mk(predicate));
  }

  static SimpleFilterNode mk(Expr predicate, RefBag refs) {
    return new SimpleFilterNodeImpl(new ExprImpl(refs, predicate.template()));
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

    final SimpleFilterNode copy = new SimpleFilterNodeImpl(predicate);
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
