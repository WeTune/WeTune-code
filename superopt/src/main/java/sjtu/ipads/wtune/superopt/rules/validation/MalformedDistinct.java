package sjtu.ipads.wtune.superopt.rules.validation;

import sjtu.ipads.wtune.superopt.operator.Distinct;
import sjtu.ipads.wtune.superopt.operator.Input;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.operator.Proj;
import sjtu.ipads.wtune.superopt.rules.BaseMatchingRule;

public class MalformedDistinct extends BaseMatchingRule {
  @Override
  public boolean enterDistinct(Distinct op) {
    final Operator in = op.predecessors()[0];
    if (in != null && !(in instanceof Proj || in instanceof Input)) {
      matched = true;
      return false;
    }
    return true;
  }
}
