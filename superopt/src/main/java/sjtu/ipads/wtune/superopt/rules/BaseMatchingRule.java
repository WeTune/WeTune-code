package sjtu.ipads.wtune.superopt.rules;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.internal.GraphVisitor;

public abstract class BaseMatchingRule implements GraphVisitor, Rule {
  protected boolean matched;

  public boolean match(Graph g) {
    g.acceptVisitor(this);
    return matched;
  }
}
