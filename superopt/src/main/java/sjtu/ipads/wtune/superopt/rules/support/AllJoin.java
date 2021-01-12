package sjtu.ipads.wtune.superopt.rules.support;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.operator.Input;
import sjtu.ipads.wtune.superopt.operator.Join;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.rules.BaseMatchingRule;

public class AllJoin extends BaseMatchingRule {
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
