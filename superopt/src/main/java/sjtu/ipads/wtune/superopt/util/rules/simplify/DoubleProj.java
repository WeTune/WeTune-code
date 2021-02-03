package sjtu.ipads.wtune.superopt.util.rules.simplify;

import sjtu.ipads.wtune.superopt.plan.Proj;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class DoubleProj extends BaseMatchingRule {
  @Override
  public boolean enterProj(Proj op) {
    if (op.predecessors()[0] instanceof Proj) {
      matched = true;
      return false;
    }
    return true;
  }
}
