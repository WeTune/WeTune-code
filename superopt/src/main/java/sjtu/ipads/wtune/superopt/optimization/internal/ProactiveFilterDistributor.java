package sjtu.ipads.wtune.superopt.optimization.internal;

import com.google.common.base.Equivalence;
import com.google.common.collect.Sets;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.Filter;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.PlainFilter;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.util.Constraints;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Equivalence.identity;
import static com.google.common.collect.Sets.combinations;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static sjtu.ipads.wtune.common.utils.Commons.listSort;
import static sjtu.ipads.wtune.common.utils.FuncUtils.collectionMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class ProactiveFilterDistributor extends FilterDistributorBase implements FilterDistributor {
  // "Proactive" means what the filter will influence the later matching
  // Example: Filter<p0 c0>(InnerJoin<c1 c2>(..)), c0=c1
  //          We have to try every possibility in this situation.

  @Override
  public void distribute(FilterDistribution dist) {
    if (!dist.isSatisfiable()) return;
    this.dist = dist;
    // ensure Subquery precedes Plain
    this.targets = listSort(targetSlots(dist), comparing(Operator::type).reversed());
    distribute0(0);
  }

  private void distribute0(int idx) {
    if (idx >= targets.size()) {
      next.distribute(dist);
      return;
    }

    final Filter target = targets.get(idx);
    final var candidates = candidatesOf(target);

    if (target.type() == OperatorType.PlainFilter) {
      final int max =
          (dist.pool().size() - dist.used().size())
              - (dist.slots().size() - dist.assigned().size() - 1);
      if (max <= 0) return;

      for (int i = 1; i <= max; i++)
        for (var assignment : combinations(candidates, i)) {
          dist.assign(target, listMap(Equivalence.Wrapper::get, assignment));
          distribute0(idx + 1);
          dist.rollback();
        }

    } else if (target.type() == OperatorType.SubqueryFilter) {
      for (var assignment : candidates) {
        dist.assign(target, singletonList(assignment.get()));
        distribute0(idx + 1);
        dist.rollback();
      }

    } else assert false;
  }

  private Set<Equivalence.Wrapper<FilterNode>> candidatesOf(Operator op) {
    final Set<Equivalence.Wrapper<FilterNode>> unused =
        collectionMap(identity()::wrap, Sets.difference(dist.pool(), dist.used()), HashSet::new);

    if (op.type() == OperatorType.SubqueryFilter)
      unused.removeIf(it -> it.get().type() == OperatorType.PlainFilter);

    return unused;
  }

  @Override
  protected boolean isTargetSlot(Filter op, FilterDistribution context) {
    final Interpretations inter = context.interpretations();
    final Constraints constraints = inter.constraints();

    final Fragment side = op.fragment();
    final Placeholder attr = op.fields();

    if (!inter.hasAssignment(attr)
        && constraints.equivalenceOf(attr).stream().anyMatch(it -> it.owner().fragment() == side))
      return true;

    if (op.type() == OperatorType.PlainFilter) {
      final Placeholder pred = ((PlainFilter) op).predicate();
      return !inter.hasAssignment(pred)
          && constraints.equivalenceOf(pred).stream().anyMatch(it -> it.owner().fragment() == side);
    }

    return false;
  }
}
