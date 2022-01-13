package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanKind;

class ReduceDedup {
  private final PlanContext plan;
  private boolean isReduced;

  ReduceDedup(PlanContext plan) {
    this.plan = plan;
    this.isReduced = false;
  }

  int reduce(int nodeId) {
    final PlanKind kind = plan.kindOf(nodeId);
    for (int i = 0, bound = kind.numChildren(); i < bound; ++i) reduce(plan.childOf(nodeId, i));

    if (kind.isSubqueryFilter()) {
      final int rhsChild = plan.childOf(nodeId, 1);
      if (plan.kindOf(rhsChild) == PlanKind.Proj) {
        isReduced = true;
        plan.infoCache().putDeduplicatedOf(rhsChild, false);
      }
    }

    return nodeId;
  }

  boolean isReduced() {
    return isReduced;
  }
}
