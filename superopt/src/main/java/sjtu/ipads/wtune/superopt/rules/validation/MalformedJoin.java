package sjtu.ipads.wtune.superopt.rules.validation;

import sjtu.ipads.wtune.superopt.operator.InnerJoin;
import sjtu.ipads.wtune.superopt.operator.LeftJoin;
import sjtu.ipads.wtune.superopt.operator.Operator;
import sjtu.ipads.wtune.superopt.rules.BaseMatchingRule;

public class MalformedJoin extends BaseMatchingRule {
  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    final Operator[] in = op.predecessors();
    if (in[0].type().isFilter() || in[1].type().isFilter()) {
      matched = true;
      return false;
    }
    return true;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    final Operator[] in = op.predecessors();
    if (in[0].type().isFilter() || in[1].type().isFilter()) {
      matched = true;
      return false;
    }
    return true;
  }
}
