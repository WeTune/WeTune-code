package sjtu.ipads.wtune.superopt.optimization.internal;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.sqlparser.plan.InputNode;
import sjtu.ipads.wtune.sqlparser.plan.PlainFilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.optimization.Fingerprint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class PlanFingerprint {
  private static final int MAX_LENGTH = 4;

  public static Iterable<String> make(PlanNode node) {
    return fingerprint0(node, MAX_LENGTH);
  }

  private static Set<String> fingerprint0(PlanNode node, int limit) {
    if (limit == 0 || node instanceof InputNode) return singleton("");
    if (node instanceof PlainFilterNode && node.successor() instanceof PlainFilterNode)
      return fingerprint0(node.predecessors()[0], limit);

    final Set<String> strings = new HashSet<>(limit);
    final PlanNode[] preds = node.predecessors();
    final char c = Fingerprint.charOf(node.type());
    if (c == '?') return emptySet();

    if (preds.length == 1)
      for (int i = 0; i < limit; i++)
        for (String sub : fingerprint0(preds[0], i)) strings.add(c + sub);
    else if (preds.length == 2)
      for (int i = 0; i < limit; i++) {
        final int j = limit - 1 - i;
        for (List<String> subs :
            Sets.cartesianProduct(fingerprint0(preds[0], i), fingerprint0(preds[1], j)))
          strings.add(c + subs.get(0) + subs.get(1));
      }
    else assert false;

    return strings;
  }
}
