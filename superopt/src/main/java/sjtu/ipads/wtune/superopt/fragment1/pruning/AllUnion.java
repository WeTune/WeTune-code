package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.fragment1.Input;
import sjtu.ipads.wtune.superopt.fragment1.Op;
import sjtu.ipads.wtune.superopt.fragment1.Union;

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
