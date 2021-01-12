package sjtu.ipads.wtune.superopt.rules.simplify;

import sjtu.ipads.wtune.superopt.operator.InnerJoin;
import sjtu.ipads.wtune.superopt.operator.LeftJoin;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.rules.BaseMatchingRule;

public class NonLeftDeepJoin extends BaseMatchingRule {
  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    final Operator right = op.predecessors()[1];
    if (right != null && !right.type().isValidOutput()) {
      matched = true;
      return false;
    }
    return true;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    final Operator right = op.predecessors()[1];
    if (right != null && !right.type().isValidOutput()) {
      matched = true;
      return false;
    }
    return true;
  }
}
