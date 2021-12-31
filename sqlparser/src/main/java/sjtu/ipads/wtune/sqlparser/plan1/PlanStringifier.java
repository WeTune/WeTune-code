package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.SYN_NAME_PREFIX;

class PlanStringifier {
  private final PlanContext plan;
  private final ValuesRegistry values;
  private final StringBuilder builder;

  PlanStringifier(PlanContext plan, StringBuilder builder) {
    this.plan = plan;
    this.values = plan.valuesReg();
    this.builder = builder;
  }

  static String stringifyNode(PlanContext ctx, int id) {
    final PlanStringifier stringifier = new PlanStringifier(ctx, new StringBuilder());
    stringifier.stringifyNode(id);
    return stringifier.builder.toString();
  }

  static String stringifyTree(PlanContext ctx, int id) {
    final PlanStringifier stringifier = new PlanStringifier(ctx, new StringBuilder());
    stringifier.stringifyTree(id);
    return stringifier.builder.toString();
  }

  private void stringifyTree(int rootId) {
    stringifyNode(rootId);
    final int numChildren = plan.kindOf(rootId).numChildren();
    final int[] children = plan.childrenOf(rootId);
    if (numChildren > 0) {
      builder.append('(');
      stringifyTree(children[0]);
    }
    if (numChildren > 1) {
      builder.append(',');
      stringifyTree(children[1]);
    }
    if (numChildren > 0) builder.append(')');
  }

  private void stringifyNode(int nodeId) {
    switch (plan.kindOf(nodeId)) {
      case SetOp -> appendSetOp(nodeId);
      case Limit -> appendLimit(nodeId);
      case Sort -> appendSort(nodeId);
      case Agg -> appendAgg(nodeId);
      case Proj -> appendProj(nodeId);
      case Filter -> appendFilter(nodeId);
      case InSub -> appendInSub(nodeId);
      case Exists -> appendExists(nodeId);
      case Join -> appendJoin(nodeId);
      case Input -> appendInput(nodeId);
    }
  }

  private void appendSetOp(int nodeId) {
    final SetOpNode setOp = (SetOpNode) plan.nodeAt(nodeId);
    builder.append(setOp.opKind()).append(nodeId);
    if (setOp.deduplicated()) builder.append('*');
  }

  private void appendLimit(int nodeId) {
    final LimitNode limit = (LimitNode) plan.nodeAt(nodeId);
    builder.append("Limit").append(nodeId).append('{');
    if (limit.limit() != null) builder.append("limit=").append(limit.limit());
    if (limit.offset() != null) builder.append("offset=").append(limit.offset());
  }

  private void appendSort(int nodeId) {
    final SortNode sort = (SortNode) plan.nodeAt(nodeId);
    builder.append("Sort").append(nodeId).append("{[");
    for (Expression expr : sort.sortSpec()) builder.append(expr);
    builder.append("],refs=[");
    for (Expression expr : sort.sortSpec()) appendRefs(expr);
    builder.append(']');
  }

  private void appendAgg(int nodeId) {
    final AggNode agg = (AggNode) plan.nodeAt(nodeId);
    final List<String> attrNames = agg.attrNames();
    final List<Expression> attrExprs = agg.attrExprs();
    final List<Expression> groupBys = agg.groupByExprs();
    final Expression having = agg.havingExpr();

    builder.append("Agg").append(nodeId).append("{[");
    appendSelectItems(attrExprs, attrNames);
    builder.append(']');
    if (!groupBys.isEmpty()) {
      builder.append(",group=[");
      for (Expression expr : groupBys) builder.append(expr);
      builder.append(']');
    }
    if (having != null) {
      builder.append(",having=").append(having);
    }
    builder.append(",refs=[");
    for (Expression expr : attrExprs) appendRefs(expr);
    for (Expression expr : groupBys) appendRefs(expr);
    if (having != null) appendRefs(having);
    builder.append("]}");
  }

  private void appendProj(int nodeId) {
    final ProjNode proj = (ProjNode) plan.nodeAt(nodeId);
    final List<Expression> exprs = proj.attrExprs();
    final List<String> names = proj.attrNames();

    builder.append("Proj");
    if (proj.deduplicated()) builder.append('*');
    builder.append(nodeId).append("{[");
    appendSelectItems(exprs, names);
    builder.append("],refs=[");
    for (Expression expr : exprs) appendRefs(expr);
    builder.append("]");
    if (proj.qualification() != null) {
      builder.append(",qual=").append(proj.qualification());
    }
    builder.append("}");
  }

  private void appendFilter(int nodeId) {
    final SimpleFilterNode filter = (SimpleFilterNode) plan.nodeAt(nodeId);
    final Expression predicate = filter.predicate();
    builder.append("Filter").append(nodeId).append('{').append(predicate).append(",refs=[");
    appendRefs(predicate);
    builder.append("]}");
  }

  private void appendInSub(int nodeId) {
    final InSubNode filter = (InSubNode) plan.nodeAt(nodeId);
    final Expression expr = filter.expr();
    builder.append("InSub").append(nodeId).append('{').append(expr).append(",refs=[");
    appendRefs(expr);
    builder.append("]}");
  }

  private void appendExists(int nodeId) {
    builder.append("Exists").append(nodeId);
  }

  private void appendJoin(int nodeId) {
    final JoinNode join = (JoinNode) plan.nodeAt(nodeId);
    final Expression joinCond = join.joinCond();
    builder.append(stringifyJoinKind(join.joinKind())).append(nodeId).append('{');
    builder.append(joinCond).append(",refs=[");
    appendRefs(joinCond);
    builder.append("]}");
  }

  private void appendInput(int nodeId) {
    final InputNode input = (InputNode) plan.nodeAt(nodeId);
    builder.append("Input").append(nodeId).append('{').append(input.table().name());
    builder.append(" AS ").append(input.qualification()).append("}");
  }

  private void appendSelectItems(List<Expression> exprs, List<String> names) {
    for (int i = 0, bound = exprs.size(); i < bound; i++) {
      final StringBuilder builder = this.builder.append(exprs.get(i));
      if (!names.get(i).startsWith(SYN_NAME_PREFIX))
        builder.append(" AS ").append(names.get(i)).append(',');
    }
  }

  private void appendRefs(Expression expr) {
    for (Value ref : values.valueRefsOf(expr)) builder.append(ref).append(',');
  }

  private static String stringifyJoinKind(JoinKind kind) {
    return switch (kind) {
      case CROSS_JOIN -> "CrossJoin";
      case INNER_JOIN -> "InnerJoin";
      case LEFT_JOIN -> "LeftJoin";
      case RIGHT_JOIN -> "RightJoin";
      case FULL_JOIN -> "FullJoin";
      default -> throw new IllegalArgumentException("unknown join kind");
    };
  }
}
