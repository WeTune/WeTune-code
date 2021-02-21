package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.plan.FilterGroupNode;
import sjtu.ipads.wtune.sqlparser.plan.InnerJoinNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.InnerJoin;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.SubqueryFilter;
import static sjtu.ipads.wtune.superopt.optimization.internal.FilterHint.rearrangeFilter;
import static sjtu.ipads.wtune.superopt.optimization.internal.JoinHint.rearrangeJoin;

public interface Hint {
  static Iterable<PlanNode> apply(PlanNode node, Operator op, Interpretations inter) {
    if (op.type() == OperatorType.Input) return singletonList(node);
    if (node.type() != op.type()
        && !(node instanceof FilterGroupNode && op.type() == SubqueryFilter)) return emptyList();

    if (node instanceof InnerJoinNode)
      return rearrangeJoin((InnerJoinNode) node, ((InnerJoin) op), inter);
    else if (node instanceof FilterGroupNode)
      return rearrangeFilter((FilterGroupNode) node, op, inter);
    else return singletonList(node);
  }
}
