package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanKind;
import sjtu.ipads.wtune.sql.plan.PlanSupport;

import static sjtu.ipads.wtune.sql.plan.PlanSupport.stringifyTree;

final class SubPlan {
  private final PlanContext plan;
  private final int nodeId;

  private final Lazy<String> stringCache;

  SubPlan(PlanContext plan, int nodeId) {
    this.plan = plan;
    this.nodeId = nodeId;
    this.stringCache = Lazy.mk(() -> stringifyTree(plan, nodeId, true));
  }

  PlanContext plan() {
    return plan;
  }

  int nodeId() {
    return nodeId;
  }

  PlanKind rootKind() {
    return plan.kindOf(nodeId);
  }

  SubPlan child(int childIdx) {
    return new SubPlan(plan, plan.childOf(nodeId, childIdx));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    final SubPlan that = (SubPlan) obj;
    return this.plan == that.plan && this.nodeId == that.nodeId;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(plan) * 31 + Integer.hashCode(nodeId);
  }

  @Override
  public String toString() {
    return stringCache.get();
  }
}
