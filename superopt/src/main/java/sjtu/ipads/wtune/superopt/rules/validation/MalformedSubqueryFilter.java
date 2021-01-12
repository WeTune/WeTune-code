package sjtu.ipads.wtune.superopt.rules.validation;

import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.operator.SubqueryFilter;
import sjtu.ipads.wtune.superopt.rules.BaseMatchingRule;

public class MalformedSubqueryFilter extends BaseMatchingRule {
  @Override
  public boolean enterSubqueryFilter(SubqueryFilter op) {
    final Operator sub = op.predecessors()[1];
    if (sub != null && !sub.type().isValidOutput()) {
      matched = true;
      return false;
    }
    return true;
  }
}
