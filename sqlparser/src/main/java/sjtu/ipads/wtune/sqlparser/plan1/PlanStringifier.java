package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;

import java.util.List;

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
      case SetOp:
        appendSetOp(nodeId);
        break;
      case Limit:
        appendLimit(nodeId);
        break;
      case Sort:
        appendSort(nodeId);
        break;
      case Agg:
        appendAgg(nodeId);
        break;
      case Proj:
        appendProj(nodeId);
        break;
      case Filter:
        appendFilter(nodeId);
        break;
      case InSub:
        appendInSub(nodeId);
        break;
      case Exists:
        appendExists(nodeId);
        break;
      case Join:
        appendJoin(nodeId);
        break;
      case Input:
        appendInput(nodeId);
        break;
    }
  }

  private void appendSetOp(int nodeId) {
    final SetOpNode setOp = (SetOpNode) plan.nodeAt(nodeId);
    builder.append(setOp.opKind());
    if (setOp.deduplicated()) builder.append('*');
  }

  private void appendLimit(int nodeId) {
    final LimitNode limit = (LimitNode) plan.nodeAt(nodeId);
    builder.append("Limit{");
    if (limit.limit() != null) builder.append("limit=").append(limit.limit());
    if (limit.offset() != null) builder.append("offset=").append(limit.offset());
  }

  private void appendSort(int nodeId) {
    final SortNode sort = (SortNode) plan.nodeAt(nodeId);
    builder.append("Sort{[");
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

    builder.append("Agg{[");
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
    builder.append(",refs=");
    for (Expression expr : attrExprs) appendRefs(expr);
    for (Expression expr : groupBys) appendRefs(expr);
    if (having != null) appendRefs(having);
  }

  private void appendProj(int nodeId) {
    final ProjNode proj = (ProjNode) plan.nodeAt(nodeId);
    final List<Expression> exprs = proj.attrExprs();
    final List<String> names = proj.attrNames();

    builder.append("Proj");
    if (proj.deduplicated()) builder.append('*');
    builder.append("{[");
    appendSelectItems(exprs, names);
    builder.append("],refs=[");
    for (Expression expr : exprs) appendRefs(expr);
    builder.append("]");
    if (proj.qualification() != null) {
      builder.append(",qual=" + proj.qualification());
    }
    builder.append("}");
  }

  private void appendFilter(int nodeId) {
    final SimpleFilterNode filter = (SimpleFilterNode) plan.nodeAt(nodeId);
    final Expression predicate = filter.predicate();
    builder.append("Filter{").append(predicate).append(",refs=[");
    appendRefs(predicate);
    builder.append("]}");
  }

  private void appendInSub(int nodeId) {
    final InSubNode filter = (InSubNode) plan.nodeAt(nodeId);
    final Expression expr = filter.expr();
    builder.append("InSub{").append(expr).append(",refs=[");
    appendRefs(expr);
    builder.append("]}");
  }

  private void appendExists(int nodeId) {
    builder.append("Exists");
  }

  private void appendJoin(int nodeId) {
    final JoinNode join = (JoinNode) plan.nodeAt(nodeId);
    final Expression joinCond = join.joinCond();
    builder.append(stringifyJoinKind(join.joinKind())).append('{');
    builder.append(joinCond).append(",refs=[");
    appendRefs(joinCond);
    builder.append("]}");
  }

  private void appendInput(int nodeId) {
    final InputNode input = (InputNode) plan.nodeAt(nodeId);
    builder.append("Input{").append(input.table().name());
    builder.append(" AS ").append(input.qualification()).append("}");
  }

  private void appendSelectItems(List<Expression> exprs, List<String> names) {
    for (int i = 0, bound = exprs.size(); i < bound; i++)
      builder.append(exprs.get(i)).append(" AS ").append(names.get(i)).append(',');
  }

  private void appendRefs(Expression expr) {
    for (Value ref : values.valueRefsOf(expr)) builder.append(ref).append(',');
  }

  private static String stringifyJoinKind(JoinKind kind) {
    switch (kind) {
      case CROSS_JOIN:
        return "CrossJoin";
      case INNER_JOIN:
        return "InnerJoin";
      case LEFT_JOIN:
        return "LeftJoin";
      case RIGHT_JOIN:
        return "RightJoin";
      case FULL_JOIN:
        return "FullJoin";
      default:
        throw new IllegalArgumentException("unknown join kind");
    }
  }
}
