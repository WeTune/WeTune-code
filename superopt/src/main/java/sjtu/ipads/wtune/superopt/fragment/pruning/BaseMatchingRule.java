package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Op;
import sjtu.ipads.wtune.superopt.fragment.OpVisitor;

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
