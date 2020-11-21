package sjtu.ipads.wtune.superopt.rules.simplify;

import sjtu.ipads.wtune.superopt.operators.Proj;
import sjtu.ipads.wtune.superopt.rules.BaseVisitorMatchingRule;

public class DoubleProj extends BaseVisitorMatchingRule {
  @Override
  public boolean enterProj(Proj op) {
    if (op.next() instanceof Proj) {
      matched = true;
      return false;
    }
    return true;
  }
}
