package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

abstract class PlanNodeBase implements PlanNode {
  protected PlanContext context;
  protected PlanNode successor;
  protected final PlanNode[] predecessors;

  protected PlanNodeBase() {
    predecessors = new PlanNode[type().numPredecessors()];
  }

  protected PlanNodeBase(OperatorType type) {
    predecessors = new PlanNode[type.numPredecessors()];
  }

  @Override
  public PlanContext context() {
    return context;
  }

  @Override
  public PlanNode successor() {
    return successor;
  }

  @Override
  public PlanNode[] predecessors() {
    return predecessors;
  }

  @Override
  public void setContext(PlanContext context) {
    if (this.context != null) throw new IllegalStateException("context is immutable once set");

    this.context = context;
  }

  @Override
  public void setSuccessor(PlanNode successor) {
    if (this.successor != null) throw new IllegalStateException("successor is immutable once set");

    this.successor = requireNonNull(successor);
  }

  @Override
  public void setPredecessor(int idx, PlanNode predecessor) {
    Objects.checkIndex(idx, predecessors.length);

    if (predecessors[idx] != null)
      throw new IllegalStateException("predecessor is immutable once set");

    predecessors[idx] = requireNonNull(predecessor);
    predecessor.setSuccessor(this);
  }

  @Override
  public PlanNode copy(PlanContext ctx) {
    final PlanNode copy = copy0(ctx);
    final PlanNode[] predecessors = this.predecessors;
    for (int i = 0, bound = predecessors.length; i < bound; i++) {
      copy.setPredecessor(i, predecessors[i].copy(ctx));
    }
    return copy;
  }

  protected abstract PlanNode copy0(PlanContext ctx);

  protected void checkContextSet() {
    if (context == null) throw new IllegalStateException("unresolved plan");
  }
}
