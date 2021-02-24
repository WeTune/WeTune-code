package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.InnerJoinNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.InnerJoin;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.superopt.optimization.internal.FilterHint.rearrangeFilter;
import static sjtu.ipads.wtune.superopt.optimization.internal.JoinHint.rearrangeJoin;

public interface Hint {
  static Iterable<PlanNode> apply(PlanNode node, Operator op, Interpretations inter) {
    final OperatorType opType = op.type(), nodeType = node.type();

    // Input can match any type of node
    if (opType == OperatorType.Input) return singletonList(node);

    // if the successor of a filter is a filter, then the filter chain must have been rearranged,
    // so just return the node
    if (opType.isFilter() && op.successor() != null && op.successor().type().isFilter())
      return singletonList(node);

    // PlainFilter can match both PlainFilter and SubqueryFilter
    // SubqueryFilter can only match SubqueryFilter
    if ((opType == OperatorType.PlainFilter && nodeType.isFilter())
        || (opType == OperatorType.SubqueryFilter && nodeType == OperatorType.SubqueryFilter))
      return rearrangeFilter((FilterNode) node, op, inter);

    // Otherwise, if operator and node are of different type, then they can never match
    if (opType != nodeType) return emptyList();

    // Rearrange plan to match inner join
    if (opType == OperatorType.InnerJoin)
      return rearrangeJoin((InnerJoinNode) node, ((InnerJoin) op), inter);

    return singletonList(node);
  }
}
