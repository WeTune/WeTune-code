package sjtu.ipads.wtune.superopt.optimizer.internal;

import static com.google.common.collect.Sets.cartesianProduct;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.SIMPLE_FILTER;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.IN_SUB_FILTER;
import static sjtu.ipads.wtune.superopt.optimizer.Fingerprint.charOf;

import java.util.HashSet;
import java.util.Set;
import sjtu.ipads.wtune.sqlparser.plan.InputNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public class PlanFingerprint {
  private static final int MAX_LENGTH = 4;

  public static Iterable<String> make(PlanNode node) {
    return fingerprint0(node, MAX_LENGTH);
  }

  private static Set<String> fingerprint0(PlanNode node, int budget) {
    // The method calculates the fingerprints of all subtrees rooted by `node`, as long as its
    // fingerprint doesn't exceed the `budget`.
    // The method is recursive. The current node combine the fingerprints of its children and then
    // prepend itself.

    // terminate if `budget` is run out or `node` is of Input.
    if (budget == 0 || node instanceof InputNode) return singleton("");

    final OperatorType type = node.kind();
    final char c = charOf(type);

    if (c == '?') return emptySet();

    final PlanNode[] preds = node.predecessors();
    final Set<String> strings = new HashSet<>(budget << 1);

    if (type.numPredecessors() == 2) {
      // for join & subquery, the budget is distributed between two children
      // invariant: i + j <= budget - 1
      for (int i = budget - 1; i >= 0; --i)
        for (int j = budget - i - 1; j >= 0; --j)
          cartesianProduct(fingerprint0(preds[0], i), fingerprint0(preds[1], j))
              .forEach(it -> strings.add(c + it.get(0) + it.get(1)));

    } else if (!type.isJoin()) {
      final char alterChar = type == IN_SUB_FILTER ? charOf(SIMPLE_FILTER) : c;
      for (int i = 0; i < budget; i++)
        for (String sub : fingerprint0(preds[0], i)) strings.add(alterChar + sub);
    }

    // special handling for filter:
    // if a filter is the child of another filter, then it can be skipped.
    if (node.kind().isFilter() && node.successor().kind().isFilter())
      strings.addAll(fingerprint0(preds[0], budget));

    return strings;
  }
}
