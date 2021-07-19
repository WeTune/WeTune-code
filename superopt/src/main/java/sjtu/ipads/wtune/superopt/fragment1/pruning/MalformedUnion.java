package sjtu.ipads.wtune.superopt.fragment1.pruning;

import sjtu.ipads.wtune.superopt.fragment1.Op;
import sjtu.ipads.wtune.superopt.fragment1.Union;

/** Rule that matches a InSubFilter with Filter or Join as its second child. */
public class MalformedUnion extends BaseMatchingRule {
  @Override
  public boolean enterUnion(Union op) {
    final Op[] in = op.predecessors();
    if (!in[0].type().isValidOutput() || in[1].type().isValidOutput()) {
      matched = true;
      return false;
    }
    return true;
  }
}
