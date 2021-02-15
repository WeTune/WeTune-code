package sjtu.ipads.wtune.superopt.util.rules.validation;

import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.SubqueryFilter;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class MalformedSubqueryFilter extends BaseMatchingRule {
  @Override
  public boolean enterSubqueryFilter(SubqueryFilter op) {
    final Operator in = op.predecessors()[1];
    if (!in.type().isValidOutput()) {
      matched = true;
      return false;
    }
    return true;
  }
}
