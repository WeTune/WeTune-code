package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Input;
import sjtu.ipads.wtune.superopt.fragment.Op;
import sjtu.ipads.wtune.superopt.fragment.Union;

/** Rule that matches a fragment with only Union operators. */
public class AllUnion extends BaseMatchingRule {
  @Override
  public boolean enter(Op op) {
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
