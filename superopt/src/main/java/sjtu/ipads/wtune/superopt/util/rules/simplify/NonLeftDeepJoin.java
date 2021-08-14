package sjtu.ipads.wtune.superopt.util.rules.simplify;

import sjtu.ipads.wtune.superopt.fragment.InnerJoin;
import sjtu.ipads.wtune.superopt.fragment.LeftJoin;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

public class NonLeftDeepJoin extends BaseMatchingRule {
  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    final Operator right = op.predecessors()[1];
    if (right != null && right.kind().isJoin()) {
      matched = true;
      return false;
    }
    return true;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    final Operator right = op.predecessors()[1];
    if (right != null && right.kind().isJoin()) {
      matched = true;
      return false;
    }
    return true;
  }
}
