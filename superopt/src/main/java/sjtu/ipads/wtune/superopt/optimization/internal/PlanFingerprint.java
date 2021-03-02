package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Sets.cartesianProduct;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.PlainFilter;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.SubqueryFilter;
import static sjtu.ipads.wtune.superopt.optimization.Fingerprint.charOf;

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

    // terminate if `budget` is run out or `node` is input.
    if (budget == 0 || node instanceof InputNode) return singleton("");

    final OperatorType type = node.type();
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
      final char alterChar = type == SubqueryFilter ? charOf(PlainFilter) : c;
      for (int i = 0; i < budget; i++)
        for (String sub : fingerprint0(preds[0], i)) strings.add(alterChar + sub);
    }

    // special handling for filter:
    // if a filter is the child of another filter, then it can be skipped.
    if (node.type().isFilter() && node.successor().type().isFilter())
      strings.addAll(fingerprint0(preds[0], budget));

    // special handling for inner join:
    // if the child of inner join is a left join, swap them and calculate its fingerprint.
    if (node instanceof InnerJoinNode && preds[0] instanceof LeftJoinNode) {
      final PlanNode nodeCopy = node.copy();
      final PlanNode predCopy = preds[0].copy();
      // Don't use `setPredecessor`, we don't want to set parent
      nodeCopy.predecessors()[0] = predCopy.predecessors()[0];
      nodeCopy.predecessors()[1] = predCopy.predecessors()[1];
      predCopy.predecessors()[0] = nodeCopy;
      predCopy.predecessors()[1] = preds[1];
      strings.addAll(fingerprint0(predCopy, budget));
    }

    return strings;
  }
}
