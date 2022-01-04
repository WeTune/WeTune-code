package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.superopt.fragment.Agg;
import sjtu.ipads.wtune.superopt.fragment.Op;

/** Rule that matches an Agg with Join as its parent. */
public class MalformedAgg extends BaseMatchingRule{
  @Override
  public boolean enterAgg(Agg op) {
    final Op successor = op.successor();
    if (successor != null && successor.kind().isJoin()) {
      matched = true;
      return false;
    }
    return true;
  }
}
