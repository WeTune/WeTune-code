package sjtu.ipads.wtune.superopt.optimizer.support;

import sjtu.ipads.wtune.sqlparser.plan.AggNode;
import sjtu.ipads.wtune.sqlparser.plan.InnerJoinNode;
import sjtu.ipads.wtune.sqlparser.plan.InputNode;
import sjtu.ipads.wtune.sqlparser.plan.LeftJoinNode;
import sjtu.ipads.wtune.sqlparser.plan.LimitNode;
import sjtu.ipads.wtune.sqlparser.plan.PlainFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;
import sjtu.ipads.wtune.sqlparser.plan.SortNode;
import sjtu.ipads.wtune.sqlparser.plan.SubqueryFilterNode;

public class TypeBasedAlgorithm<T> {
  protected T dispatch(PlanNode node) {
    switch (node.type()) {
      case Input:
        return onInput((InputNode) node);
      case PlainFilter:
        return onPlainFilter((PlainFilterNode) node);
      case SubqueryFilter:
        return onSubqueryFilter((SubqueryFilterNode) node);
      case InnerJoin:
        return onInnerJoin((InnerJoinNode) node);
      case LeftJoin:
        return onLeftJoin((LeftJoinNode) node);
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

  protected T onPlainFilter(PlainFilterNode filter) {
    return null;
  }

  protected T onSubqueryFilter(SubqueryFilterNode filter) {
    return null;
  }

  protected T onProj(ProjNode proj) {
    return null;
  }

  protected T onInnerJoin(InnerJoinNode innerJoin) {
    return null;
  }

  protected T onLeftJoin(LeftJoinNode leftJoin) {
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
