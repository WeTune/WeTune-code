package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.superopt.fragment1.InnerJoin;
import sjtu.ipads.wtune.superopt.fragment1.LeftJoin;
import sjtu.ipads.wtune.superopt.fragment1.Op;

/** Rule that matches a Join with Filter as its second child. */
public class MalformedJoin extends BaseMatchingRule {
  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    final Op[] in = op.predecessors();
    if (in[0].kind().isFilter() || in[1].kind().isFilter()) {
      matched = true;
      return false;
    }
    return true;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    final Op[] in = op.predecessors();
    if (in[0].kind().isFilter() || in[1].kind().isFilter()) {
      matched = true;
      return false;
    }
    return true;
  }
}
