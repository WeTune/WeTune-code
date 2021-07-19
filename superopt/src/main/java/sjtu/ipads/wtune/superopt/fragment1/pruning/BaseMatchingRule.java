package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.fragment1.OpVisitor;

public abstract class BaseMatchingRule implements OpVisitor, Rule {
  protected boolean matched;

  public boolean match(Fragment g) {
    g.acceptVisitor(this);
    return matched;
  }
}
