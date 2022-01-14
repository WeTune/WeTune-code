package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.superopt.fragment.Agg;

import static sjtu.ipads.wtune.superopt.fragment.OpKind.AGG;

public class TooManyAgg extends BaseMatchingRule {
  @Override
  public boolean enterAgg(Agg op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  private static boolean checkOverwhelming(Agg op) {
    if (op.successor() != null && op.successor().kind() == AGG)
      return true;

    return false;
  }
}
