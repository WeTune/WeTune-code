package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

class SimpleFilterNodeImpl extends PlanNodeBase implements SimpleFilterNode {
  protected final Expr predicate;

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
  public PlanNode copy(PlanContext ctx) {
    checkContextSet();

    final SimpleFilterNode copy = new SimpleFilterNodeImpl(predicate);
    copy.setContext(ctx);

    ctx.registerRefs(copy, refs());
    for (Ref ref : refs()) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public StringBuilder stringify0(StringBuilder builder, boolean compact) {
    builder.append("Filter{");
    builder.append(predicate);
    stringifyRefs(builder, compact);
    builder.append('}');
    stringifyChildren(builder, compact);
    return builder;
  }
}
