package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment1.SimpleFilter;

public class TooManySimpleFilter extends BaseMatchingRule {
  @Override
  public boolean enterSimpleFilter(SimpleFilter op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  private static boolean checkOverwhelming(SimpleFilter op) {
    if (op.predecessors()[0] == null || op.predecessors()[0].kind() != OperatorType.SIMPLE_FILTER)
      return false;

    return op.successor() != null || !isInput(op.predecessors()[0].predecessors()[0]);
  }
}
