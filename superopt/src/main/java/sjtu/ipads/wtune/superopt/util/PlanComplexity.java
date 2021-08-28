package sjtu.ipads.wtune.superopt.util;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;

class PlanComplexity implements Complexity {
  private final int[] opCounts;

  PlanComplexity(PlanNode tree) {
    this.opCounts = new int[OperatorType.values().length + 1];
    countOps(tree);
  }

  private void countOps(PlanNode op) {
    ++opCounts[op.kind().ordinal()];
    // Treat deduplication as an operator.
    if (op.kind() == OperatorType.PROJ && ((ProjNode) op).isDeduplicated())
      ++opCounts[opCounts.length - 1];

    for (PlanNode predecessor : op.predecessors()) countOps(predecessor);
  }

  @Override
  public int[] opCounts() {
    return opCounts;
  }
}
