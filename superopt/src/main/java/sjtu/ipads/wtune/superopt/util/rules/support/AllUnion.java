package sjtu.ipads.wtune.superopt.util.rules.support;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Input;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.Union;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

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
  public boolean match(Fragment g) {
    matched = true;
    g.acceptVisitor(this);
    return matched;
  }
}
