package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;
import java.util.NoSuchElementException;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFlatMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

class SortNodeImpl extends PlanNodeBase implements SortNode {
  private final List<Expr> orders;
  private final RefBag refs;
  private int[] refHints;

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
  public void setRefHints(int[] refHints) {
    requireNonNull(refHints);
    this.refHints = refHints;
  }

  @Override
  public boolean rebindRefs(PlanContext refCtx) {
    try {
      rebindRefs(refCtx, refs(), predecessors[0].predecessors()[0]);
      return true;
    } catch (NoSuchElementException ignored) {
    }

    try {
      rebindRefs(refCtx, refs(), predecessors[0]);
      return true;
    } catch (NoSuchElementException notFound2) {
      final ValueBag inValues = predecessors[0].values();
      for (int i = 0; i < refHints.length; i++) {
        final int hint = refHints[i];
        if (hint == -1) return false;
        else {
          final Value refValue = inValues.get(hint);
          final Value usedValue =
              refValue.expr().isIdentity()
                  ? context().deRef(refValue.expr().refs().get(0))
                  : refValue;
          this.context().setRef(refs.get(i), usedValue);
        }
      }
      return true;
    }
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
