package sjtu.ipads.wtune.superopt.util.rules.support;

import sjtu.ipads.wtune.superopt.plan.Input;
import sjtu.ipads.wtune.superopt.plan.Join;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class AllJoin extends BaseMatchingRule {
  @Override
  public boolean enter(PlanNode op) {
    if (!(op instanceof Join) && !(op instanceof Input)) {
      matched = false;
      return false;
    }
    return true;
  }

  @Override
  public boolean match(Plan g) {
    matched = true;
    g.acceptVisitor(this);
    return matched;
  }
}
