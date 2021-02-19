package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import sjtu.ipads.wtune.sqlparser.plan.InputNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.InputInterpretation;

public class InputInterpretationImpl extends InterpretationBase<PlanNode>
    implements InputInterpretation {
  public InputInterpretationImpl(PlanNode input) {
    super(input);
  }

  @Override
  public boolean isCompatible(PlanNode otherNode) {
    final PlanNode thisNode = object();
    if (thisNode == otherNode) return true;
    if (otherNode.type() != thisNode.type()) return false;
    if (otherNode.type() != OperatorType.Input) return false;
    return ((InputNode) otherNode).table().equals(((InputNode) thisNode).table());
  }
}
