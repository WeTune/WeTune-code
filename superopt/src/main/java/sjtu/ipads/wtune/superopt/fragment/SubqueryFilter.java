package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.SubqueryFilterNode;
import sjtu.ipads.wtune.superopt.fragment.internal.SubqueryFilterImpl;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public interface SubqueryFilter extends Operator {
  Placeholder fields();

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
    final PlanNode node = SubqueryFilterNode.make(inter.getAttributes(fields()).object().getLeft());
    node.setPredecessor(0, pred0);
    node.setPredecessor(1, pred1);
    node.resolveUsedTree();
    return node;
  }

  @Override
  default boolean match(PlanNode node, Interpretations inter) {
    if (node.type() != this.type()) return false;

    return inter.assignAttributes(fields(), ((SubqueryFilterNode) node).usedAttributes());
  }
}
