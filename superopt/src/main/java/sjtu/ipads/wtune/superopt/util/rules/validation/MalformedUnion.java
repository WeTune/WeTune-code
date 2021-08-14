package sjtu.ipads.wtune.superopt.util.rules.validation;

import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.Union;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class MalformedUnion extends BaseMatchingRule {
  @Override
  public boolean enterUnion(Union op) {
    final Operator[] in = op.predecessors();
    if (isInvalidInput(in[0]) || isInvalidInput(in[1])) {
      matched = true;
      return false;
    }
    return true;
  }

  private static boolean isInvalidInput(Operator in) {
    return in != null && !in.kind().isValidOutput();
  }
}
