package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.superopt.fragment1.InnerJoin;
import sjtu.ipads.wtune.superopt.fragment1.LeftJoin;
import sjtu.ipads.wtune.superopt.fragment1.Op;

/** Rule that matches non-left-deep Join tree. */
public class NonLeftDeepJoin extends BaseMatchingRule {
  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    final Op right = op.predecessors()[1];
    if (right != null && right.type().isJoin()) {
      matched = true;
      return false;
    }
    return true;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    final Op right = op.predecessors()[1];
    if (right != null && right.type().isJoin()) {
      matched = true;
      return false;
    }
    return true;
  }
}
