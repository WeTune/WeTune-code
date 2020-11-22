package sjtu.ipads.wtune.superopt.rules.support;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.operators.Input;
import sjtu.ipads.wtune.superopt.operators.Join;
import sjtu.ipads.wtune.superopt.rules.BaseVisitorMatchingRule;

public class AllJoinRule extends BaseVisitorMatchingRule {
  @Override
  public boolean enter(Operator op) {
    if (!(op instanceof Join) && !(op instanceof Input)) {
      matched = false;
      return false;
    }
    return true;
  }

  @Override
  public boolean match(Graph g) {
    matched = true;
    g.acceptVisitor(this);
    return matched;
  }
}
