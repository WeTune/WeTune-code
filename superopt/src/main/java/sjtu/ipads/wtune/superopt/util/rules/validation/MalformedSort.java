package sjtu.ipads.wtune.superopt.util.rules.validation;

import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.Sort;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class MalformedSort extends BaseMatchingRule {
  @Override
  public boolean enterSort(Sort op) {
    final Operator in = op.predecessors()[0];
    if ((in != null && !in.type().isValidOutput()) || (in instanceof Sort)) {
      matched = true;
      return false;
    }
    return true;
  }
}
