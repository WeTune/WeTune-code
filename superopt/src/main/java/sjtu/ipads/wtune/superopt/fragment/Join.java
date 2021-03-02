package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

import java.util.List;

public interface Join extends Operator {
  Placeholder leftFields();

  Placeholder rightFields();

  @Override
  default PlanNode instantiate(Interpretations interpretations) {
    final PlanNode pred0 = predecessors()[0].instantiate(interpretations);
    final PlanNode pred1 = predecessors()[1].instantiate(interpretations);

    final List<AttributeDef> l = interpretations.getAttributes(leftFields()).object();
    final List<AttributeDef> r = interpretations.getAttributes(rightFields()).object();

    final PlanNode node =
        type() == OperatorType.LeftJoin ? LeftJoinNode.make(l, r) : InnerJoinNode.make(l, r);

    node.setPredecessor(0, pred0);
    node.setPredecessor(1, pred1);
    return node;
  }

  @Override
  default boolean match(PlanNode node, Interpretations inter) {
    if (node.type() != this.type()) return false;

    final JoinNode join = (JoinNode) node;
    if (!join.isNormalForm()) return false;

    return inter.assignAttributes(leftFields(), join.leftAttributes())
        && inter.assignAttributes(rightFields(), join.rightAttributes());
  }
}
