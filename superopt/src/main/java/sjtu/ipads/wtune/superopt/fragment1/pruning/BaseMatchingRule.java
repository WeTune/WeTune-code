package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.fragment1.Op;
import sjtu.ipads.wtune.superopt.fragment1.OpVisitor;

public abstract class BaseMatchingRule implements OpVisitor, Rule {
  protected boolean matched;

  public boolean match(Fragment g) {
    matched = false;

    g.acceptVisitor(this);
    return matched;
  }

  protected static boolean isInput(Op op) {
    return op == null || op.kind() == OperatorType.INPUT;
  }
}
