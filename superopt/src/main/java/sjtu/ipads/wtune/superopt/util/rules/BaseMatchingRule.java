package sjtu.ipads.wtune.superopt.util.rules;

import sjtu.ipads.wtune.superopt.plan.Plan;
import sjtu.ipads.wtune.superopt.plan.PlanVisitor;

public abstract class BaseMatchingRule implements PlanVisitor, Rule {
  protected boolean matched;

  public boolean match(Plan g) {
    g.acceptVisitor(this);
    return matched;
  }
}
