package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Join;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;
import static sjtu.ipads.wtune.superopt.optimization.internal.FilterHint.rearrangeFilter;
import static sjtu.ipads.wtune.superopt.optimization.internal.JoinHint.rearrangeJoinNew;

public interface Hint {
  static Iterable<PlanNode> apply(PlanNode node, Operator op, Interpretations inter) {
    final OperatorType opType = op.type(), nodeType = node.type();

    // Input can match any type of node
    if (opType == Input) return singletonList(node);

    // Filter rearrangement only happens on filter chain head. otherwise do nothing
    if (opType.isFilter() && op.successor() != null && op.successor().type().isFilter())
      return singletonList(node);

    // PlainFilter can match both PlainFilter and SubqueryFilter
    // SubqueryFilter can only match SubqueryFilter
    if ((opType == PlainFilter && nodeType.isFilter())
        || (opType == SubqueryFilter && nodeType == SubqueryFilter))
      return rearrangeFilter((FilterNode) node, op, inter);

    // Join rearrangement
    if (opType.isJoin() && node.type().isJoin())
      return rearrangeJoinNew((JoinNode) node, (Join) op, inter);

    // Otherwise, the type of op and the type of node are required to be identical
    return opType != nodeType ? emptyList() : singletonList(node);
  }
}
