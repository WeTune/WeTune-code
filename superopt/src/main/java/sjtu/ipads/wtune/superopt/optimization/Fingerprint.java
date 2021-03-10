package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.optimization.internal.FragmentFingerprint;
import sjtu.ipads.wtune.superopt.optimization.internal.PlanFingerprint;

public interface Fingerprint {
  static String make(Fragment g) {
    return FragmentFingerprint.make(g);
  }

  static Iterable<String> make(PlanNode node) {
    return PlanFingerprint.make(node);
  }

  static char charOf(OperatorType t) {
    switch (t) {
      case Proj:
        return 'p';
      case InnerJoin:
      case LeftJoin:
        return 'j';
      case SubqueryFilter:
        return 's';
      case PlainFilter:
        return 'f';
      default:
        return '?';
    }
  }
}
