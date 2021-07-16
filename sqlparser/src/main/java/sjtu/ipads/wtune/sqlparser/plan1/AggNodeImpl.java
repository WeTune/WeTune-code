package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

class AggNodeImpl extends PlanNodeBase implements AggNode {
  private final ValueBag values;
  private final List<Expr> groups;
  private final Expr having;

  private final RefBag refs;

  AggNodeImpl(ValueBag values, List<Expr> groups, Expr having, RefBag refs) {
    this.values = values;
    this.groups = requireNonNull(groups);
    this.having = having;
    this.refs = refs;
  }

  static AggNode build(List<ASTNode> selectItems, List<ASTNode> groupNodes, ASTNode havingNode) {
    final List<Value> values = listMap(selectItems, ExprValue::fromSelectItem);
    final List<Expr> groups =
        groupNodes == null ? emptyList() : listMap(groupNodes, ExprImpl::build);
    final Expr having = havingNode == null ? null : ExprImpl.build(havingNode);

    final List<Ref> refs =
        new ArrayList<>(values.size() + groups.size() + (having == null ? 0 : 1));

    // replace the refs in exprs
    int acc = 0;
    for (Value value : values) {
      final List<Ref> newRefs = replaceRefs(value.expr(), acc);
      refs.addAll(newRefs);
      acc += newRefs.size();
    }

    for (Expr expr : groups) {
      final List<Ref> newRefs = replaceRefs(expr, acc);
      refs.addAll(newRefs);
      acc += newRefs.size();
    }

    if (having != null) {
      final List<Ref> newRefs = replaceRefs(having, acc);
      refs.addAll(newRefs);
      acc += newRefs.size();
    }

    assert acc == refs.size();

    return new AggNodeImpl(new ValueBagImpl(values), groups, having, new RefBagImpl(refs));
  }

  @Override
  public List<Expr> groups() {
    return groups;
  }

  @Override
  public Expr having() {
    return having;
  }

  @Override
  public ValueBag values() {
    return values;
  }

  @Override
  public RefBag refs() {
    return refs;
  }

  @Override
  protected PlanNode copy0(PlanContext ctx) {
    checkContextSet();

    final AggNode copy = new AggNodeImpl(values, groups, having, refs);
    copy.setContext(ctx);

    ctx.registerRefs(copy, refs);
    ctx.registerValues(copy, values);
    for (Ref ref : refs) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("Agg{").append('[');
    for (Value value : values) {
      builder.append(value.expr());
      final String str = value.toString();
      if (!str.isEmpty()) builder.append(" AS ").append(str);
    }
    builder.append(']');
    if (!groups.isEmpty()) builder.append(",groups=").append(groups);
    if (having != null) builder.append(",having=").append(having);
    if (!refs.isEmpty()) {
      builder.append(",refs=");
      if (context == null) builder.append(refs);
      else builder.append(context.deRef(refs));
    }
    builder.append('}');

    if (predecessors[0] != null) builder.append('(').append(predecessors[0]).append(')');

    return builder.toString();
  }

  private static List<Ref> replaceRefs(Expr expr, int startIdx) {
    final int numRefs = expr.refs().size();
    final List<Ref> newRefs =
        IntStream.range(startIdx, startIdx + numRefs)
            .mapToObj(it -> new RefImpl(null, "agg_key_" + it))
            .collect(Collectors.toList());
    expr.setRefs(newRefs);
    return newRefs;
  }
}
