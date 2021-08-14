package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;

public abstract class BaseOperator implements Operator {
  private Fragment fragment;
  private Operator successor;
  private final Operator[] predecessors;

  protected BaseOperator() {
    predecessors = new Operator[kind().numPredecessors()];
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
  public Fragment fragment() {
    return fragment;
  }

  @Override
  public void setSuccessor(Operator successor) {
    this.successor = successor;
  }

  @Override
  public void setPredecessor(int idx, Operator predecessor) {
    this.predecessors[idx] = predecessor;
    if (predecessor != null) predecessor.setSuccessor(this);
  }

  @Override
  public void setFragment(Fragment fragment) {
    this.fragment = fragment;
  }

  @Override
  public void acceptVisitor(OperatorVisitor visitor) {
    if (visitor.enter(this)) {
      if (accept0(visitor)) {

        final Operator[] prevs = predecessors();
        for (int i = 0; i < prevs.length; i++) {
          final Operator prev = prevs[i];
          if (prev != null) prev.acceptVisitor(visitor);
          else visitor.enterEmpty(this, i);
        }
      }

      leave0(visitor);
    }
    visitor.leave(this);
  }

  @Override
  public String toString() {
    return kind().name();
  }

  protected abstract Operator newInstance();

  protected abstract boolean accept0(OperatorVisitor visitor);

  protected abstract void leave0(OperatorVisitor visitor);
}
