package sjtu.ipads.wtune.superopt.util.rules.validation;

import sjtu.ipads.wtune.superopt.plan.PlanNode;
import sjtu.ipads.wtune.superopt.plan.Union;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class MalformedUnion extends BaseMatchingRule {
  @Override
  public boolean enterUnion(Union op) {
    final PlanNode[] in = op.predecessors();
    if (isInvalidInput(in[0]) || isInvalidInput(in[1])) {
      matched = true;
      return false;
    }
    return true;
  }

  private static boolean isInvalidInput(PlanNode in) {
    return in != null && !in.type().isValidOutput();
  }
}
