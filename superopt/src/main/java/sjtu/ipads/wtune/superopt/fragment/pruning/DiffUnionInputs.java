package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.superopt.fragment.Op;
import sjtu.ipads.wtune.superopt.fragment.Union;

public class DiffUnionInputs extends BaseMatchingRule{
  @Override
  public boolean enterUnion(Union op) {
    Op in0 = op.predecessors()[0], in1 = op.predecessors()[1];
    if (in0 != null && in1 != null) {
      if (in0.kind().isFilter() && in1.kind().isFilter()) return true;
      if (in0.kind().isJoin() && in1.kind().isJoin()) return true;
      if (in0.kind().isSubquery() && in1.kind().isSubquery()) return true;
      if (in0.kind() != in1.kind()) {
        matched = true;
        return false;
      }
    }
    return true;
  }
}
