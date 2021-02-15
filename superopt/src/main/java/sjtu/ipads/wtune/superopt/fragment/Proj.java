package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;
import sjtu.ipads.wtune.superopt.fragment.internal.ProjImpl;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

import java.util.List;

public interface Proj extends Operator {
  Placeholder fields();

  @Override
  default OperatorType type() {
    return OperatorType.Proj;
  }

  static Proj create() {
    return ProjImpl.create();
  }

  @Override
  default PlanNode instantiate(Interpretations interpretations) {
    final PlanNode pred = predecessors()[0].instantiate(interpretations);
    final List<OutputAttribute> projs = interpretations.getAttributes(fields()).object();
    final ProjNode node = ProjNode.make(projs);
    node.setPredecessor(0, pred);
    node.resolveUsedAttributes();
    return node;
  }

  @Override
  default boolean match(PlanNode node, Interpretations inter) {
    if (node.type() != this.type()) return false;

    return inter.assignAttributes(fields(), node.outputAttributes());
  }
}
