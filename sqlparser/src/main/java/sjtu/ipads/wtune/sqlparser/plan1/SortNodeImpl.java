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

  static SortNode mk(List<ASTNode> orders) {
    final List<Expr> exprs = listMap(orders, ExprImpl::mk);
    return new SortNodeImpl(exprs, RefBag.mk(listFlatMap(exprs, Expr::refs)));
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
  public StringBuilder stringify(StringBuilder builder) {
    builder.append("Sort{");
    stringifyRefs(builder);
    builder.append('}');
    stringifyChildren(builder);
    return builder;
  }
}
