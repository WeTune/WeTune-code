package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.InputInterpretation;

import java.util.Objects;

public class InputInterpretationImpl extends InterpretationBase<PlanNode>
    implements InputInterpretation {
  public InputInterpretationImpl(PlanNode input) {
    super(input);
  }

  @Override
  public boolean isCompatible(PlanNode otherNode) {
    return Objects.equals(object(), otherNode);
  }
}
