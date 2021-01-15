package sjtu.ipads.wtune.superopt.solving;

import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

public class Relation {
  private final Value[] output;
  private final Proposition condition;
  private final int offset;
  private final Value[] tuples;

  public Relation(Proposition condition, Value[] output, int offset, Value[] tuples) {
    this.output = output;
    this.condition = condition;
    this.offset = offset;
    this.tuples = tuples;
  }

  public Value[] output() {
    return output;
  }

  public Proposition condition() {
    return condition;
  }

  public int offset() {
    return offset;
  }

  public Value[] tuples() {
    return tuples;
  }
}
