package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.superopt.fragment.Agg;

public class TooDeepAgg extends BaseMatchingRule {
  @Override
  public boolean enterAgg(Agg op) {
    if (op.successor() != null && op.successor().successor() != null) {
      matched = true;
      return false;
    }
    return true;
  }
}
