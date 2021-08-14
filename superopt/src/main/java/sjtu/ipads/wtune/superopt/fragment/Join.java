package sjtu.ipads.wtune.superopt.fragment;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public interface Join extends Operator {
  Placeholder leftFields();

  Placeholder rightFields();

  @Override
  default PlanNode instantiate(Interpretations interpretations) {
    final PlanNode pred0 = predecessors()[0].instantiate(interpretations);
    final PlanNode pred1 = predecessors()[1].instantiate(interpretations);

    final List<AttributeDef> l = interpretations.getAttributes(leftFields()).object();
    final List<AttributeDef> r = interpretations.getAttributes(rightFields()).object();

    final PlanNode node = JoinNode.make(kind(), l, r);

    node.setPredecessor(0, pred0);
    node.setPredecessor(1, pred1);
    return node;
  }

  @Override
  default boolean match(PlanNode node, Interpretations inter) {
    if (node.kind() != this.kind()) return false;

    final JoinNode join = (JoinNode) node;
    if (!join.isNormalForm()) return false;

    return inter.assignAttributes(leftFields(), join.leftAttributes())
        && inter.assignAttributes(rightFields(), join.rightAttributes());
  }
}
