package sjtu.ipads.wtune.superopt.util.rules.validation;

import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.SubqueryFilter;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class MalformedSubqueryFilter extends BaseMatchingRule {
  @Override
  public boolean enterSubqueryFilter(SubqueryFilter op) {
    final PlanNode in = op.predecessors()[1];
    if (!in.type().isValidOutput()) {
      matched = true;
      return false;
    }
    return true;
  }
}
