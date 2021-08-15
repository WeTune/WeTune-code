package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment1.Proj;

public class TooManyProj extends BaseMatchingRule {

  @Override
  public boolean enterProj(Proj op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  private static boolean checkOverwhelming(Proj op) {
    if (op.predecessors()[0] == null || op.predecessors()[0].kind() != OperatorType.PROJ)
      return false;

    return op.successor() != null || !isInput(op.predecessors()[0].predecessors()[0]);
  }
}
