package sjtu.ipads.wtune.superopt.util.rules.validation;

import sjtu.ipads.wtune.superopt.plan.Limit;
import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class MalformedLimit extends BaseMatchingRule {
  @Override
  public boolean enterLimit(Limit op) {
    final PlanNode in = op.predecessors()[0];
    if ((in != null && !in.type().isValidOutput()) || (in instanceof Limit)) {
      matched = true;
      return false;
    }
    return true;
  }
}
