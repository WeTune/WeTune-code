package sjtu.ipads.wtune.superopt.rules.validation;

import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.Distinct;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.operators.Proj;
import sjtu.ipads.wtune.superopt.operators.Union;
import sjtu.ipads.wtune.superopt.rules.BaseVisitorMatchingRule;

public class MalformedDistinct extends BaseVisitorMatchingRule {
  @Override
  public boolean enterDistinct(Distinct op) {
    final Operator in = op.prev()[0];
    if (in != null && !(in instanceof Proj || in instanceof Union || in instanceof Input)) {
      matched = true;
      return false;
    }
    return true;
  }
}
