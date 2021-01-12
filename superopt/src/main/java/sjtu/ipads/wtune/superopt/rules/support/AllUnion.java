package sjtu.ipads.wtune.superopt.rules.support;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.operator.Input;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.operator.Union;
import sjtu.ipads.wtune.superopt.rules.BaseMatchingRule;

public class AllUnion extends BaseMatchingRule {
  @Override
  public boolean enter(Operator op) {
    if (!(op instanceof Union) && !(op instanceof Input)) {
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
