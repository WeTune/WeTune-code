package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.optimizer.internal.FragmentFingerprint;
import sjtu.ipads.wtune.superopt.optimizer.internal.PlanFingerprint;

public interface Fingerprint {
  static String make(Fragment g) {
    return FragmentFingerprint.make(g);
  }

  static Iterable<String> make(PlanNode node) {
    return PlanFingerprint.make(node);
  }

  static char charOf(OperatorType t) {
    switch (t) {
      case PROJ:
        return 'p';
      case INNER_JOIN:
      case LEFT_JOIN:
        return 'j';
      case IN_SUB_FILTER:
        return 's';
      case SIMPLE_FILTER:
        return 'f';
      default:
        return '?';
    }
  }
}
