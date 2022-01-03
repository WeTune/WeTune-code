package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.superopt.fragment.OpKind;
import sjtu.ipads.wtune.superopt.fragment.Union;

public class TooManyUnion extends BaseMatchingRule {

  @Override
  public boolean enterUnion(Union op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  private static boolean checkOverwhelming(Union op) {
    return op.successor() != null && op.successor().kind() == OpKind.SET_OP;
  }
}
