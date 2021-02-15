package sjtu.ipads.wtune.superopt.util.rules;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;

public abstract class BaseMatchingRule implements OperatorVisitor, Rule {
  protected boolean matched;

  public boolean match(Fragment g) {
    g.acceptVisitor(this);
    return matched;
  }
}
