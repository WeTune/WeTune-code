package sjtu.ipads.wtune.superopt.optimization.match.internal;

import sjtu.ipads.wtune.superopt.optimization.match.InputInterpretation;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public class InputInterpretationImpl implements InputInterpretation {
  private final PlanNode input;

  public InputInterpretationImpl(PlanNode input) {
    this.input = input;
  }

  @Override
  public PlanNode operator() {
    return input;
  }

  boolean isCompatible(PlanNode other) {
    return other == input;
  }
}
