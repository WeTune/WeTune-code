package sjtu.ipads.wtune.sqlparser.plan;

public class TypeBasedAlgorithm<T> {
  public T dispatch(PlanNode node) {
    switch (node.type()) {
      case INPUT:
        return onInput((InputNode) node);
      case SIMPLE_FILTER:
        return onPlainFilter((FilterNode) node);
      case IN_SUB_FILTER:
        return onSubqueryFilter((FilterNode) node);
      case INNER_JOIN:
        return onInnerJoin((JoinNode) node);
      case LEFT_JOIN:
        return onLeftJoin((JoinNode) node);
      case PROJ:
        return onProj((ProjNode) node);
      case LIMIT:
        return onLimit((LimitNode) node);
      case SORT:
        return onSort((SortNode) node);
      case AGG:
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
