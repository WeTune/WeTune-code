package sjtu.ipads.wtune.superopt.rules;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.GraphVisitor;

public abstract class BaseVisitorMatchingRule implements GraphVisitor, Rule {
  protected boolean matched;

  @Override
  public boolean match(Graph g) {
    g.acceptVisitor(this);
    return matched;
  }
}
