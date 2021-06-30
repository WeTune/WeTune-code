package sjtu.ipads.wtune.sqlparser.plan;

public class TypeBasedAlgorithm<T> {
  public T dispatch(PlanNode node) {
    switch (node.type()) {
      case Input:
        return onInput((InputNode) node);
      case PlainFilter:
        return onPlainFilter((FilterNode) node);
      case InSubFilter:
        return onSubqueryFilter((FilterNode) node);
      case InnerJoin:
        return onInnerJoin((JoinNode) node);
      case LeftJoin:
        return onLeftJoin((JoinNode) node);
      case Proj:
        return onProj((ProjNode) node);
      case Limit:
        return onLimit((LimitNode) node);
      case Sort:
        return onSort((SortNode) node);
      case Agg:
        return onAgg((AggNode) node);
      default:
        throw new IllegalArgumentException();
    }
  }

  protected T onInput(InputNode input) {
    return null;
  }

  protected T onPlainFilter(FilterNode filter) {
    return null;
  }

  protected T onSubqueryFilter(FilterNode filter) {
    return null;
  }

  protected T onProj(ProjNode proj) {
    return null;
  }

  protected T onInnerJoin(JoinNode innerJoin) {
    return null;
  }

  protected T onLeftJoin(JoinNode leftJoin) {
    return null;
  }

  protected T onLimit(LimitNode limit) {
    return null;
  }

  protected T onSort(SortNode sort) {
    return null;
  }

  protected T onAgg(AggNode agg) {
    return null;
  }
}
