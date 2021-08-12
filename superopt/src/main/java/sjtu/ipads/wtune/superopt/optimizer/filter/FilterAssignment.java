package sjtu.ipads.wtune.superopt.optimizer.filter;

import static sjtu.ipads.wtune.common.utils.Commons.listConcat;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.Expr;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Filter;

public record FilterAssignment(Filter op, List<FilterNode> used) {
  public FilterNode assignment() {
    assert !used.isEmpty();

    used.sort(Comparator.comparing(it -> it.predicate().toString()));

    if (used.size() == 1) {
      final FilterNode node = (FilterNode) Iterables.getOnlyElement(used).copy();

      if (node.type() == OperatorType.IN_SUB_FILTER)
        node.setPredecessor(1, PlanNode.copyOnTree(node.predecessors()[1]));
      return node;

    } else return Lists.reverse(used).stream().reduce(FilterAssignment::combineFilters).get();
  }

  private static FilterNode combineFilters(FilterNode f0, FilterNode f1) {
    final Expr expr0 = f0.predicate();
    final Expr expr1 = f1.predicate();

    return FilterNode.makePlainFilter(Expr.combine(expr0, expr1), listConcat(f0.usedAttributes(), f1.usedAttributes()));
  }
}
