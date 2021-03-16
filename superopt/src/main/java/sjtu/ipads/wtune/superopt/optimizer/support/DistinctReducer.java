package sjtu.ipads.wtune.superopt.optimizer.support;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;

import static sjtu.ipads.wtune.superopt.optimizer.support.UniquenessInference.inferUniqueness;

public class DistinctReducer {
  public static boolean reduceDistinct(PlanNode node) {
    if (node.type() == OperatorType.Input) return false;

    final boolean reduced = reduceDistinct(node.predecessors()[0]);
    if (node.type() == OperatorType.Proj) {
      final ProjNode proj = (ProjNode) node;
      if (proj.isForcedUnique()) {
        proj.setForcedUnique(false);
        if (inferUniqueness(proj)) return true;
        else proj.setForcedUnique(true);
      }
    }

    return reduced;
  }
}
