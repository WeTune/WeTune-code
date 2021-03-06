package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Collections2.orderedPermutations;
import static com.google.common.collect.Sets.difference;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static sjtu.ipads.wtune.common.utils.Commons.newIdentitySet;
import static sjtu.ipads.wtune.common.utils.Commons.tail;

public class FreeFilterDistributor extends FilterDistributorBase implements FilterDistributor {
  // "Free" means the filter is not involved in any constrained, thus never influence the match.
  // All the remaining filters are assigned to them, arbitrarily.

  @Override
  public void distribute(FilterDistribution dist) {
    if (!dist.isSatisfiable()) return;
    this.dist = dist;
    this.targets = targetSlots(dist);

    if (targets.isEmpty()) {
      dist.saveResult();
      return;
    }

    final Collection<List<FilterNode>> groups =
        difference(dist.pool(), dist.used()).stream()
            .collect(Collectors.groupingBy(it -> sourceOf(it.usedAttributes())))
            .values();
    final var perms = orderedPermutations(groups, comparing(System::identityHashCode));

    if (groups.size() >= targets.size()) distribute0(perms);
    else distribute1(perms);
  }

  private void distribute0(Collection<List<List<FilterNode>>> perms) {
    for (List<List<FilterNode>> perm : perms) {
      for (int i = 0, bound = targets.size() - 1; i < bound; i++)
        dist.assign(targets.get(i), new ArrayList<>(perm.get(i)));

      dist.assign(tail(targets), new ArrayList<>(difference(dist.pool(), dist.used())));
      dist.saveResult();

      for (int i = 0, bound = targets.size(); i < bound; i++) dist.rollback();
    }
  }

  private void distribute1(Collection<List<List<FilterNode>>> perms) {
    for (List<List<FilterNode>> perm : perms) {
      for (int i = 0, bound = perm.size(); i < bound; i++)
        dist.assign(targets.get(i), new ArrayList<>(perm.get(i)));
      final List<FilterAssignment> results = dist.assignments();

      reDistribute(
          results.subList(results.size() - perm.size(), results.size()),
          targets.subList(targets.size() - perm.size(), targets.size()),
          0);

      for (int i = 0, bound = perm.size(); i < bound; i++) dist.rollback();
    }
  }

  private void reDistribute(List<FilterAssignment> assignments, List<Filter> spilled, int idx) {
    if (idx > spilled.size()) {
      dist.saveResult();
      return;
    }

    final Filter op = spilled.get(idx);
    for (FilterAssignment assignment : assignments) {
      final Collection<FilterNode> used = assignment.used();
      if (used.size() <= 1) continue;

      assert used instanceof List;
      final List<FilterNode> nodes = (List<FilterNode>) used;
      final FilterNode stolen = nodes.remove(nodes.size() - 1);
      dist.assign(op, singletonList(stolen));

      reDistribute(assignments, spilled, idx + 1);

      dist.rollback();
      nodes.add(stolen);
    }
  }

  private static Set<PlanNode> sourceOf(List<AttributeDef> attrs) {
    final Set<PlanNode> sources = newIdentitySet();
    for (AttributeDef attr : attrs) {
      AttributeDef upstream = attr;
      while (true) {
        final AttributeDef tmp = upstream.upstream();
        if (tmp == null || tmp == upstream) break;
        upstream = tmp;
      }
      sources.add(upstream.definer());
    }
    return sources;
  }
}
