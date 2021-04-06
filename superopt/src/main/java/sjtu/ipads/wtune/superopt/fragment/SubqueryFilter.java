package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.internal.SubqueryFilterImpl;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

public interface SubqueryFilter extends Filter {
  @Override
  default OperatorType type() {
    return OperatorType.SubqueryFilter;
  }

  static SubqueryFilter create() {
    return SubqueryFilterImpl.create();
  }

  @Override
  default PlanNode instantiate(Interpretations inter) {
    final PlanNode pred0 = predecessors()[0].instantiate(inter);
    final PlanNode pred1 = predecessors()[1].instantiate(inter);
    final PlanNode node = FilterNode.makeSubqueryFilter(inter.getAttributes(fields()).object());
    node.setPredecessor(0, pred0);
    node.setPredecessor(1, pred1);
    return node;
  }

  @Override
  default boolean match(PlanNode node, Interpretations inter) {
    if (node.type() != this.type()) return false;

    return inter.assignAttributes(fields(), node.usedAttributes());
  }
}
