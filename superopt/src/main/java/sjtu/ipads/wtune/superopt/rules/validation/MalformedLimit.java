package sjtu.ipads.wtune.superopt.rules.validation;

import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Limit;
import sjtu.ipads.wtune.superopt.rules.BaseVisitorMatchingRule;

public class MalformedLimit extends BaseVisitorMatchingRule {
  @Override
  public boolean enterLimit(Limit op) {
    final Operator in = op.prev()[0];
    if ((in != null && !in.canBeQueryOut()) || (in instanceof Limit)) {
      matched = true;
      return false;
    }
    return true;
  }
}
