package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.superopt.fragment.InSubFilter;
import sjtu.ipads.wtune.superopt.fragment.OpKind;

public class TooManySubqueryFilter extends BaseMatchingRule {
  @Override
  public boolean enterInSubFilter(InSubFilter op) {
    matched = checkOverwhelming(op);
    return !matched;
  }

  private static boolean checkOverwhelming(InSubFilter op) {
    if (op.predecessors()[0] == null || op.predecessors()[0].kind() != OpKind.IN_SUB_FILTER)
      return false;

    return op.successor() != null
        || !isInput(op.predecessors()[1])
        || !isInput(op.predecessors()[0].predecessors()[0])
        || !isInput(op.predecessors()[0].predecessors()[1]);
  }
}
