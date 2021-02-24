package sjtu.ipads.wtune.superopt.util.rules.support;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Input;
import sjtu.ipads.wtune.superopt.fragment.Join;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

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
  public boolean match(Fragment g) {
    final Operator head = g.head();

    if (head.type().isJoin()
        && head.predecessors()[0] instanceof Input
        && head.predecessors()[1] instanceof Input) return false;

    matched = true;
    g.acceptVisitor(this);
    return matched;
  }
}
