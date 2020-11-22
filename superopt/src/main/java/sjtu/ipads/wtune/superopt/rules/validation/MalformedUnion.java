package sjtu.ipads.wtune.superopt.rules.validation;

import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Union;
import sjtu.ipads.wtune.superopt.rules.BaseVisitorMatchingRule;

public class MalformedUnion extends BaseVisitorMatchingRule {
  @Override
  public boolean enterUnion(Union op) {
    final Operator[] in = op.prev();
    if (isInvalidInput(in[0]) || isInvalidInput(in[1])) {
      matched = true;
      return false;
    }
    return true;
  }

  private static boolean isInvalidInput(Operator in) {
    return in != null && !in.canBeQueryOut();
  }
}
