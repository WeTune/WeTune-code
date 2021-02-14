package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.superopt.optimization.Operator;

public abstract class OperatorBase implements Operator {
  private Operator successor;
  private final Operator[] predecessors;

  protected OperatorBase() {
    predecessors = new Operator[type().numPredecessors()];
  }

  @Override
  public Operator successor() {
    return successor;
  }

  @Override
  public Operator[] predecessors() {
    return predecessors;
  }

  @Override
  public void setPredecessor(int idx, Operator op) {
    predecessors[idx] = op;
    if (op != null) op.setSuccessor(this);
  }

  @Override
  public void setSuccessor(Operator successor) {
    this.successor = successor;
  }
}
