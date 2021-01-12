package sjtu.ipads.wtune.superopt.rules.validation;

import sjtu.ipads.wtune.superopt.operator.*;
import sjtu.ipads.wtune.superopt.rules.BaseMatchingRule;

public class MalformedJoin extends BaseMatchingRule {
  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    final Operator[] in = op.predecessors();
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
