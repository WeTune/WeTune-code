package sjtu.ipads.wtune.superopt.util.rules.support;

import sjtu.ipads.wtune.superopt.plan.Input;
import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.Union;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class AllUnion extends BaseMatchingRule {
  @Override
  public boolean enter(PlanNode op) {
    if (!(op instanceof Union) && !(op instanceof Input)) {
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
