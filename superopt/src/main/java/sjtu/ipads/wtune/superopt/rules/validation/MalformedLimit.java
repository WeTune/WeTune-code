package sjtu.ipads.wtune.superopt.rules.validation;

import sjtu.ipads.wtune.superopt.operator.Limit;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.rules.BaseMatchingRule;

public class MalformedLimit extends BaseMatchingRule {
  @Override
  public boolean enterLimit(Limit op) {
    final Operator in = op.predecessors()[0];
    if ((in != null && !in.type().isValidOutput()) || (in instanceof Limit)) {
      matched = true;
      return false;
    }
    return true;
  }
}
