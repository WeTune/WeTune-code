package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.superopt.fragment1.InnerJoin;
import sjtu.ipads.wtune.superopt.fragment1.Join;
import sjtu.ipads.wtune.superopt.fragment1.LeftJoin;

public class TooManyJoin extends BaseMatchingRule {
  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  private static boolean checkOverwhelming(Join op) {
    return op.successor() != null && op.successor().kind().isJoin();
  }
}
