package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.superopt.fragment1.InSubFilter;
import sjtu.ipads.wtune.superopt.fragment1.Op;

/** Rule that matches a InSubFilter with Filter or Join as its second child. */
public class MalformedSubquery extends BaseMatchingRule {
  @Override
  public boolean enterSubqueryFilter(InSubFilter op) {
    final Op in = op.predecessors()[1];
    if (!in.kind().isValidOutput()) {
      matched = true;
      return false;
    }
    return true;
  }
}
