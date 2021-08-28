package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.ConstraintAwareModel;
import sjtu.ipads.wtune.superopt.fragment.Filter;
import sjtu.ipads.wtune.superopt.fragment.Join;
import sjtu.ipads.wtune.superopt.fragment.Op;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INPUT;

public interface ReversedMatch<P extends PlanNode, O extends Op> {
  List<P> reverseMatch(P plan, O op, ConstraintAwareModel model);

  List<P> results();

  static ReversedMatch<JoinNode, Join> forJoin() {
    return new JoinReversedMatch();
  }

  static ReversedMatch<FilterNode, Filter> forFilter() {
    return new FilterReversedMatch();
  }

  static List<? extends PlanNode> reversedMatch(PlanNode node, Op op, ConstraintAwareModel model) {
    final OperatorType opType = op.kind(), nodeType = node.kind();

    // Input can match any type of node
    if (opType == INPUT) return singletonList(node);

    // Filter reversed match only happens on chain head. Otherwise, do nothing.
    if (opType.isFilter() && op.successor() != null && op.successor().kind().isFilter())
      return singletonList(node);

    if (nodeType.isFilter() && opType.isFilter())
      return forFilter().reverseMatch((FilterNode) node, (Filter) op, model);

    // Join rearrangement
    if (nodeType.isJoin() && opType.isJoin())
      return forJoin().reverseMatch((JoinNode) node, (Join) op, model);

    // Otherwise, the type of op and the type of node are required to be identical
    return opType != nodeType ? emptyList() : singletonList(node);
  }
}
