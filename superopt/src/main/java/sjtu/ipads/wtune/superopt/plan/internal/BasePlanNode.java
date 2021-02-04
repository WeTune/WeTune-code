package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public abstract class BasePlanNode implements PlanNode {
  private Plan plan;
  private final PlanNode[] predecessors;

  protected BasePlanNode() {
    predecessors = new PlanNode[type().numPredecessors()];
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
  public void setPredecessor(int idx, PlanNode prev) {
    this.predecessors[idx] = prev;
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

  protected Placeholder makePlaceholder(String tag) {
    return new PlaceholderImpl(this, tag);
  }

  protected abstract PlanNode newInstance();

  abstract boolean accept0(PlanVisitor visitor);

  abstract void leave0(PlanVisitor visitor);
}
