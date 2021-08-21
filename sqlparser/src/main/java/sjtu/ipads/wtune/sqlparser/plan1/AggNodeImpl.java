package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

class AggNodeImpl extends PlanNodeBase implements AggNode {
  private final ValueBag values;
  private final List<Expr> groups;
  private final Expr having;

  private final RefBag aggRefs, groupRefs, havingRefs;

  AggNodeImpl(
      ValueBag values,
      List<Expr> groups,
      Expr having,
      RefBag aggRefs,
      RefBag groupRefs,
      RefBag havingRefs) {
    this.values = values;
    this.groups = requireNonNull(groups);
    this.having = having;
    this.aggRefs = aggRefs;
    this.groupRefs = groupRefs;
    this.havingRefs = havingRefs;
  }

  static AggNode mk(List<ASTNode> selectItems, List<ASTNode> groupNodes, ASTNode havingNode) {
    final List<Value> values = listMap(selectItems, ExprValue::fromSelectItem);
    final List<Expr> groups = groupNodes == null ? emptyList() : listMap(groupNodes, ExprImpl::mk);
    final Expr having = havingNode == null ? null : ExprImpl.mk(havingNode);

    final List<Ref> aggRefs = new ArrayList<>(values.size());
    final List<Ref> groupRefs = new ArrayList<>(groups.size());
    final List<Ref> havingRefs = new ArrayList<>(having == null ? 0 : 1);

    // replace the refs in exprs
    int acc = 0;
    for (Value value : values) acc += replaceRefs(value.expr(), acc, aggRefs);
    for (Expr expr : groups) acc += replaceRefs(expr, acc, groupRefs);
    if (having != null) acc += replaceRefs(having, acc, havingRefs);

    assert acc == aggRefs.size() + groupRefs.size() + havingRefs.size();

    return new AggNodeImpl(
        ValueBag.mk(values),
        groups,
        having,
        RefBag.mk(aggRefs),
        RefBag.mk(groupRefs),
        RefBag.mk(havingRefs));
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
  public RefBag aggRefs() {
    return aggRefs;
  }

  @Override
  public RefBag groupRefs() {
    return groupRefs;
  }

  @Override
  public RefBag havingRefs() {
    return havingRefs;
  }

  @Override
  public RefBag refs() {
    return RefBag.mk(listJoin(aggRefs, groupRefs, havingRefs));
  }

  @Override
  public PlanNode copy(PlanContext ctx) {
    checkContextSet();

    final AggNode copy = new AggNodeImpl(values, groups, having, aggRefs, groupRefs, havingRefs);
    copy.setContext(ctx);

    final RefBag refs = refs();
    ctx.registerRefs(copy, refs);
    ctx.registerValues(copy, values);
    for (Ref ref : refs) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public StringBuilder stringify0(StringBuilder builder) {
    builder.append("Agg{");

    builder.append('[');
    joining(",", values, builder, this::stringifyAsSelectItem);
    builder.append(']');

    if (!groups.isEmpty()) builder.append(",groups=").append(groups);
    if (having != null) builder.append(",having=").append(having);
    stringifyRefs(builder);

    builder.append('}');

    stringifyChildren(builder);

    return builder;
  }

  private static int replaceRefs(Expr expr, int startIdx, List<Ref> buffer) {
    final int numRefs = expr.refs().size();
    IntStream.range(startIdx, startIdx + numRefs)
        .mapToObj(it -> new RefImpl(null, "agg_key_" + it))
        .forEach(buffer::add);
    expr.setRefs(buffer.subList(startIdx, startIdx + numRefs));
    return numRefs;
  }
}
