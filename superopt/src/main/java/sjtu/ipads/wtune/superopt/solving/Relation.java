package sjtu.ipads.wtune.superopt.solving;

import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

public class Relation {
  private final Value[] output;
  private final Proposition condition;

  public Relation(Proposition condition, Value[] output) {
    this.output = output;
    this.condition = condition;
  }

  public Value[] output() {
    return output;
  }

  public Proposition condition() {
    return condition;
  }
}
