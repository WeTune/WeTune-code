package sjtu.ipads.wtune.superopt.fragment.matching;

import sjtu.ipads.wtune.sqlparser.plan.FilterGroupNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

import static java.util.Collections.singletonList;

public interface Hint {
  static Iterable<PlanNode> apply(PlanNode node, Operator op, Interpretations inter) {
    if (node instanceof FilterGroupNode) node = ((FilterGroupNode) node).filters().get(0);
    return singletonList(node);
  }
}
