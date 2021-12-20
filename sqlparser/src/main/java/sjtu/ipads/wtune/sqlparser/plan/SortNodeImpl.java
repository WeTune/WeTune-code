package sjtu.ipads.wtune.sqlparser.plan;

import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.ListSupport.flatMap;

class SortNodeImpl extends PlanNodeBase implements SortNode {
  private final List<Expr> orders;
  private final RefBag refs;
  private int[] refHints;

  SortNodeImpl(List<Expr> orders, RefBag refs) {
    this.orders = orders;
    this.refs = refs;
  }

  static SortNode mk(List<ASTNode> orders) {
    final List<Expr> exprs = ListSupport.map((Iterable<ASTNode>) orders, (Function<? super ASTNode, ? extends Expr>) ExprImpl::mk);
    return new SortNodeImpl(exprs, RefBag.mk(flatMap(exprs, Expr::refs)));
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
  public void setRefHints(int[] refHints) {
    requireNonNull(refHints);
    this.refHints = refHints;
  }

  @Override
  public boolean rebindRefs(PlanContext refCtx) {
    // Sort can reference to the attributes exposed by deeper node.
    // For example: Select a.i From a Order By a.j
    final PlanNode input0 = predecessors()[0].predecessors()[0];
    final PlanNode input1 = predecessors()[0];
    return rebindRefs(refCtx, refs(), new TIntArrayList(refHints), input0, input1);
  }

  @Override
  public PlanNode copy(PlanContext ctx) {
    checkContextSet();

    final SortNode copy = new SortNodeImpl(orders, refs);
    copy.setContext(ctx);
    copy.setRefHints(refHints);

    ctx.registerRefs(copy, refs);
    for (Ref ref : refs) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public List<Expr> orders() {
    return orders;
  }

  @Override
  public StringBuilder stringify0(StringBuilder builder, boolean compact) {
    builder.append("Sort{");
    stringifyRefs(builder, compact);
    builder.append('}');
    stringifyChildren(builder, compact);
    return builder;
  }
}
