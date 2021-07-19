package sjtu.ipads.wtune.superopt.fragment;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;
import sjtu.ipads.wtune.superopt.fragment.internal.ProjImpl;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public interface Proj extends Operator {
  Placeholder fields();

  @Override
  default OperatorType type() {
    return OperatorType.PROJ;
  }

  @Override
  default PlanNode instantiate(Interpretations interpretations) {
    final PlanNode pred = predecessors()[0].instantiate(interpretations);
    final List<AttributeDef> pair = interpretations.getAttributes(fields()).object();
    final ProjNode node = ProjNode.make(pair);
    node.setPredecessor(0, pred);
    return node;
  }

  @Override
  default boolean match(PlanNode node, Interpretations inter) {
    if (node.type() != this.type()) return false;
    return inter.assignAttributes(fields(), node.definedAttributes());
  }

  static Proj create() {
    return ProjImpl.create();
  }
}
