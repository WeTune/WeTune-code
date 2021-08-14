package sjtu.ipads.wtune.superopt.optimizer.filter;

import static java.util.Collections.singletonList;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.Expr;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.Filter;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.PlainFilter;
import sjtu.ipads.wtune.superopt.fragment.SubqueryFilter;

public class DecidedFilterDistributor extends FilterDistributorBase implements FilterDistributor {
  // "Decided" meaning what the filter should be has be decided according to existing assignment and
  // constraints.
  // Example: Proj<c0>(Filter<p0 c1>(Input<t1)), c0=c1
  //          `c1` must have been decided because `c0` was decided.

  @Override
  protected boolean isTargetSlot(Filter op, FilterDistribution ctx) {
    if (op.kind() == OperatorType.SIMPLE_FILTER) {
      final PlainFilter filterOp = (PlainFilter) op;
      return ctx.interpretations().hasAssignment(filterOp.fields())
          || ctx.interpretations().hasAssignment(filterOp.predicate());

    } else if (op.kind() == OperatorType.IN_SUB_FILTER) {
      return ctx.interpretations().hasAssignment(op.fields());

    } else assert false;
    return false;
  }

  @Override
  public void distribute(FilterDistribution dist) {
    this.dist = dist;
    this.targets = targetSlots(this.dist);
    distribute0(0);
  }

  private void distribute0(int idx) {
    if (idx >= targets.size()) {
      next.distribute(dist);
      return;
    }

    final Filter target = targets.get(idx);
    for (FilterNode node : candidatesOf(target)) {
      dist.assign(target, singletonList(node));
      distribute0(idx + 1);
      dist.rollback();
    }
  }

  private Collection<FilterNode> candidatesOf(Operator op) {
    final Collection<FilterNode> unused = Sets.difference(dist.pool(), dist.used());
    final Collection<FilterNode> candidates = new ArrayList<>(unused.size());

    if (op.kind() == OperatorType.SIMPLE_FILTER) {
      final PlainFilter filterOp = (PlainFilter) op;

      final Expr expr = dist.interpretations().interpretPredicate(filterOp.predicate());
      final List<AttributeDef> attrs =
          dist.interpretations().interpretAttributes(filterOp.fields());

      for (FilterNode node : unused)
        if ((attrs == null || attrs.equals(node.usedAttributes()))
            && (expr == null || expr.equals(node.predicate()))) candidates.add(node);
      // TODO: support the case where several filter nodes collectively satisfy the constraint
      return candidates;

    } else if (op.kind() == OperatorType.IN_SUB_FILTER) {
      final List<AttributeDef> attrs =
          dist.interpretations().interpretAttributes(((SubqueryFilter) op).fields());
      for (FilterNode node : unused)
        if (node.kind() == OperatorType.IN_SUB_FILTER && node.usedAttributes().equals(attrs))
          candidates.add(node);
      return candidates;

    } else assert false;

    return Collections.emptyList();
  }
}
