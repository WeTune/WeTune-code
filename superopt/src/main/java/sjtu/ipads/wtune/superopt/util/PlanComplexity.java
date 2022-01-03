package sjtu.ipads.wtune.superopt.util;

import sjtu.ipads.wtune.sql.ast1.constants.JoinKind;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanKind;
import sjtu.ipads.wtune.superopt.fragment.OpKind;

import static sjtu.ipads.wtune.sql.plan.PlanSupport.isDedup;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.joinKindOf;
import static sjtu.ipads.wtune.superopt.fragment.OpKind.INNER_JOIN;
import static sjtu.ipads.wtune.superopt.fragment.OpKind.LEFT_JOIN;

public class PlanComplexity implements Complexity {
  private final int[] opCounts;

  PlanComplexity(PlanContext plan, int rootId) {
    this.opCounts = new int[OpKind.values().length + 1];
    countOps(plan, rootId);
  }

  private void countOps(PlanContext plan, int nodeId) {
    final PlanKind nodeKind = plan.kindOf(nodeId);
    if (nodeKind == PlanKind.Join) {
      final JoinKind joinKind = joinKindOf(plan, nodeId);
      if (joinKind.isInner()) ++opCounts[INNER_JOIN.ordinal()];
      else ++opCounts[LEFT_JOIN.ordinal()];

    } else {
      if (nodeKind.ordinal() > PlanKind.Join.ordinal()) ++opCounts[nodeKind.ordinal() + 1];
      else ++opCounts[nodeKind.ordinal()];
      // Treat deduplication as an operator.
      if (nodeKind == PlanKind.Proj && isDedup(plan, nodeId)) ++opCounts[opCounts.length - 1];
    }

    for (int i = 0, bound = nodeKind.numChildren(); i < bound; i++)
      countOps(plan, plan.childOf(nodeId, i));
  }

  @Override
  public int[] opCounts() {
    return opCounts;
  }
}
