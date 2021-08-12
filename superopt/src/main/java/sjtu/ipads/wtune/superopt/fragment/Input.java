package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.internal.InputImpl;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public interface Input extends Operator {
  static Input create() {
    return InputImpl.create();
  }

  Placeholder table();

  @Override
  default OperatorType type() {
    return OperatorType.INPUT;
  }

  @Override
  default boolean match(PlanNode node, Interpretations inter) {
    return inter.assignInput(table(), node);
  }

  @Override
  default PlanNode instantiate(Interpretations interpretations) {
    return PlanNode.copyOnTree(interpretations.getInput(table()).object());
  }
}
