package sjtu.ipads.wtune.superopt.optimizer.support;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.LITERAL_VALUE;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INPUT;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.LIMIT;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.PROJ;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.SORT;
import static sjtu.ipads.wtune.superopt.optimizer.support.UniquenessInference.inferUniqueness;

import sjtu.ipads.wtune.sqlparser.plan.LimitNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;

public class DistinctReducer {
  public static boolean reduceDistinct(PlanNode node) {
    if (node.type() == INPUT) return false;

    final boolean reduced = reduceDistinct(node.predecessors()[0]);
    if (node.type() == PROJ) {
      final ProjNode proj = (ProjNode) node;
      if (proj.isForcedUnique()) {
        proj.setForcedUnique(false);
        if (inferUniqueness(proj) || isLimitedAsSingleton(node)) return true;
        else proj.setForcedUnique(true);
      }
    }

    return reduced;
  }

  private static boolean isLimitedAsSingleton(PlanNode proj) {
    assert proj.type() == PROJ;
    PlanNode successor = proj.successor();
    if (successor != null && successor.type() == SORT) successor = successor.successor();
    if (successor != null && successor.type() == LIMIT) {
      final LimitNode limit = (LimitNode) successor;
      return limit.offset() == null && Integer.valueOf(1).equals(limit.limit().get(LITERAL_VALUE));
    }
    return false;
  }
}
