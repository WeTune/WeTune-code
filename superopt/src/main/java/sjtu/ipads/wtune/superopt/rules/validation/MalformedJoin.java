package sjtu.ipads.wtune.superopt.rules.validation;

import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Join;
import sjtu.ipads.wtune.superopt.operators.PlainFilter;
import sjtu.ipads.wtune.superopt.operators.SubqueryFilter;
import sjtu.ipads.wtune.superopt.rules.BaseVisitorMatchingRule;

public class MalformedJoin extends BaseVisitorMatchingRule {
  @Override
  public boolean enterJoin(Join op) {
    final Operator[] in = op.prev();
    if (isInvalidInput(in[0]) || isInvalidInput(in[1])) {
      matched = true;
      return false;
    }
    return true;
  }

  private static boolean isInvalidInput(Operator other) {
    return other instanceof PlainFilter || other instanceof SubqueryFilter;
  }
}
