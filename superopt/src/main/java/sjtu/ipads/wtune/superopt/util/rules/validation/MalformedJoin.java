package sjtu.ipads.wtune.superopt.util.rules.validation;

import sjtu.ipads.wtune.superopt.fragment.InnerJoin;
import sjtu.ipads.wtune.superopt.fragment.LeftJoin;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.util.rules.BaseMatchingRule;

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
