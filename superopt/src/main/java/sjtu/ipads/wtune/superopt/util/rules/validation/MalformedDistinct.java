package sjtu.ipads.wtune.superopt.util.rules.validation;

import sjtu.ipads.wtune.superopt.fragment.Distinct;
import sjtu.ipads.wtune.superopt.fragment.Input;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.Proj;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

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
