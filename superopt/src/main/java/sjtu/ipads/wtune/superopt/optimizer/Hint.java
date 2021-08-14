package sjtu.ipads.wtune.superopt.optimizer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INPUT;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.SIMPLE_FILTER;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.IN_SUB_FILTER;
import static sjtu.ipads.wtune.superopt.optimizer.filter.FilterHint.rearrangeFilter;
import static sjtu.ipads.wtune.superopt.optimizer.join.JoinHint.rearrangeJoinNew;

import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Join;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

public interface Hint {
  static Iterable<PlanNode> apply(PlanNode node, Operator op, Interpretations inter) {
    final OperatorType opType = op.kind(), nodeType = node.kind();

    // Input can match any type of node
    if (opType == INPUT) return singletonList(node);

    // Filter rearrangement only happens on filter chain head. otherwise do nothing
    if (opType.isFilter() && op.successor() != null && op.successor().kind().isFilter())
      return singletonList(node);

    // PlainFilter can match both PlainFilter and SubqueryFilter
    // SubqueryFilter can only match SubqueryFilter
    if ((opType == SIMPLE_FILTER && nodeType.isFilter())
        || (opType == IN_SUB_FILTER && nodeType == IN_SUB_FILTER))
      return rearrangeFilter((FilterNode) node, op, inter);

    // Join rearrangement
    if (opType.isJoin() && node.kind().isJoin())
      return rearrangeJoinNew((JoinNode) node, (Join) op, inter);

    // Otherwise, the type of op and the type of node are required to be identical
    return opType != nodeType ? emptyList() : singletonList(node);
  }
}
