package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public abstract class BasePlanNode implements PlanNode {
  private Plan plan;
  private PlanNode successor;
  private final PlanNode[] predecessors;

  protected BasePlanNode() {
    predecessors = new PlanNode[type().numPredecessors()];
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
  public Plan plan() {
    return plan;
  }

  @Override
  public void setSuccessor(PlanNode successor) {
    this.successor = successor;
  }

  @Override
  public void setPredecessor(int idx, PlanNode predecessor) {
    this.predecessors[idx] = predecessor;
    if (predecessor != null) predecessor.setSuccessor(this);
  }

  @Override
  public void setPlan(Plan plan) {
    this.plan = plan;
  }

  @Override
  public void acceptVisitor(PlanVisitor visitor) {
    if (visitor.enter(this)) {
      if (accept0(visitor)) {

        final PlanNode[] prevs = predecessors();
        for (int i = 0; i < prevs.length; i++) {
          final PlanNode prev = prevs[i];
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
    return type().name();
  }

  protected abstract PlanNode newInstance();

  protected abstract boolean accept0(PlanVisitor visitor);

  protected abstract void leave0(PlanVisitor visitor);
}
