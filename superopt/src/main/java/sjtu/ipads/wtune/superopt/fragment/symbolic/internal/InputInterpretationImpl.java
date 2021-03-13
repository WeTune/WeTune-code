package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.InputInterpretation;

import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.toStringOnTree;

public class InputInterpretationImpl extends InterpretationBase<PlanNode>
    implements InputInterpretation {
  public InputInterpretationImpl(PlanNode input) {
    super(input);
  }

  @Override
  public boolean isCompatible(PlanNode otherNode) {
    return toStringOnTree(object()).equals(toStringOnTree(otherNode));
  }
}
