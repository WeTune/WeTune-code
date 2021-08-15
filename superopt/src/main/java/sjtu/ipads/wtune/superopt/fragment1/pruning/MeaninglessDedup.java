package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment1.Op;
import sjtu.ipads.wtune.superopt.fragment1.Proj;

public class MeaninglessDedup extends BaseMatchingRule {
  @Override
  public boolean enterProj(Proj op) {
    if (!op.isDeduplicated()) return true;

    final Op successor = op.successor();
    if (successor == null || !successor.kind().isSubquery() || successor.predecessors()[1] != op)
      return true;

    if (successor.successor() != null
        || (op.predecessors()[0] != null && op.predecessors()[0].kind() != OperatorType.INPUT)) {
      matched = true;
      return false;
    }
    return true;
  }
}
