package sjtu.ipads.wtune.superopt.optimization.match.internal;

import sjtu.ipads.wtune.superopt.optimization.match.InputInterpretation;
import sjtu.ipads.wtune.superopt.optimization.Operator;

public class InputInterpretationImpl implements InputInterpretation {
  private final Operator input;

  public InputInterpretationImpl(Operator input) {
    this.input = input;
  }

  @Override
  public Operator input() {
    return input;
  }

  boolean isCompatible(Operator other) {
    return other == input;
  }
}
