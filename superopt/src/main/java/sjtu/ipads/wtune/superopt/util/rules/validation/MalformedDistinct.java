package sjtu.ipads.wtune.superopt.util.rules.validation;

import sjtu.ipads.wtune.superopt.plan.Distinct;
import sjtu.ipads.wtune.superopt.plan.Input;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.Proj;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class MalformedDistinct extends BaseMatchingRule {
  @Override
  public boolean enterDistinct(Distinct op) {
    final PlanNode in = op.predecessors()[0];
    if (in != null && !(in instanceof Proj || in instanceof Input)) {
      matched = true;
      return false;
    }
    return true;
  }
}
