package sjtu.ipads.wtune.superopt.util.rules.simplify;

import sjtu.ipads.wtune.superopt.plan.InnerJoin;
import sjtu.ipads.wtune.superopt.plan.LeftJoin;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class NonLeftDeepJoin extends BaseMatchingRule {
  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    final PlanNode right = op.predecessors()[1];
    if (right != null && right.type().isJoin()) {
      matched = true;
      return false;
    }
    return true;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    final PlanNode right = op.predecessors()[1];
    if (right != null && right.type().isJoin()) {
      matched = true;
      return false;
    }
    return true;
  }
}
