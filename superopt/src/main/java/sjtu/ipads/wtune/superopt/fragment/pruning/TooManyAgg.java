package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.Agg;

public class TooManyAgg extends BaseMatchingRule {
  @Override
  public boolean enterAgg(Agg op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  private static boolean checkOverwhelming(Agg op) {
    if (op.successor() != null && op.successor().kind() == OperatorType.AGG)
      return true;
    if (op.successor() != null
        && op.successor().successor() != null
        && op.successor().successor().kind() == OperatorType.AGG)
      return true;

    return false;
  }
}
