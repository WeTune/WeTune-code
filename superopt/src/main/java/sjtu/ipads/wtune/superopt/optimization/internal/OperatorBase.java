package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.superopt.optimization.Operator;

public abstract class OperatorBase implements Operator {
  private final Operator[] predecessors;

  protected OperatorBase() {
    predecessors = new Operator[type().numPredecessors()];
  }

  @Override
  public Operator[] predecessors() {
    return predecessors;
  }
}
