package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listFlatMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

class SortNodeImpl extends PlanNodeBase implements SortNode {
  private final List<Expr> orders;
  private final RefBag refs;

  SortNodeImpl(List<Expr> orders, RefBag refs) {
    this.orders = orders;
    this.refs = refs;
  }

  static SortNode build(List<ASTNode> orders) {
    final List<Expr> exprs = listMap(orders, ExprImpl::build);
    return new SortNodeImpl(exprs, new RefBagImpl(listFlatMap(Expr::refs, exprs)));
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

    final SortNode copy = new SortNodeImpl(orders, refs);
    copy.setContext(ctx);

    ctx.registerRefs(copy, refs);
    for (Ref ref : refs) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public List<Expr> orders() {
    return orders;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("Sort{").append(orders);
    if (!refs.isEmpty()) {
      builder.append(",refs=");
      if (context == null) builder.append(refs);
      else builder.append(context.deRef(refs));
    }
    builder.append('{');
    if (predecessors[0] != null) builder.append('(').append(predecessors[0]).append(')');
    return builder.toString();
  }
}
