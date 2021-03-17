package sjtu.ipads.wtune.superopt.optimizer.filter;

import static com.google.common.collect.Collections2.orderedPermutations;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.Filter;
import sjtu.ipads.wtune.superopt.fragment.PlainFilter;
import sjtu.ipads.wtune.superopt.util.Constraints;

public class BoundedFilterDistributor extends FilterDistributorBase implements FilterDistributor {
  // "Bounded" means what what the filter is bounded by constraints among filters.
  // Example: Filter<p0 c0>(Filter<p1 c1>(Input<t0>)), p0=p1 /\ c0=c1
  //          The class distribute filters according to the fact: two filter must be identical.

  @Override
  public void distribute(FilterDistribution dist) {
    if (!dist.isSatisfiable()) return;

    this.dist = dist;
    this.targets = targetSlots(dist);

    distributeByPredicate();
  }

  private void distributeByPredicate() {
    final List<List<Filter>> opEqClasses =
        eqClassesByPredicate(targets, dist.interpretations().constraints());
    final List<List<FilterNode>> nodeEqClasses =
        eqClassesByPredicate(Sets.difference(dist.pool(), dist.used()));

    distribute0(opEqClasses, nodeEqClasses, this::distributeByAttribute);
  }

  private void distributeByAttribute() {
    final List<List<Filter>> opEqClasses =
        eqClassesByAttributes(targets, dist.interpretations().constraints());
    final List<List<FilterNode>> nodeEqClasses = eqClassesByAttributes(dist.used());

    distribute0(opEqClasses, nodeEqClasses, () -> next.distribute(dist));
  }

  private void distribute0(
      List<List<Filter>> opEqClasses, List<List<FilterNode>> nodeEqClasses, Runnable continuation) {
    if (opEqClasses.size() > nodeEqClasses.size()) return;
    if (opEqClasses.isEmpty()) {
      continuation.run();
      return;
    }

    for (List<List<FilterNode>> perm0 :
        orderedPermutations(nodeEqClasses, comparing(System::identityHashCode)))
      for (int i = 0, bound0 = opEqClasses.size(); i < bound0; i++) {
        final List<Filter> opEqClass = opEqClasses.get(i);
        final List<FilterNode> nodeEqClass = perm0.get(i);

        if (opEqClass.size() > nodeEqClass.size()) continue;

        outer:
        for (List<FilterNode> perm1 :
            orderedPermutations(nodeEqClass, comparing(System::identityHashCode))) {

          for (int j = 0, bound1 = opEqClass.size(); j < bound1; j++) {
            final Filter op = opEqClass.get(j);
            final FilterNode node = perm1.get(j);

            final FilterAssignment assignment = dist.assignmentOf(op);
            // conflict: 1. the operator has been assigned by another node
            //           2. the node has been assigned to another operator
            if ((assignment != null && !assignment.used().equals(singleton(node)))
                || (assignment == null && dist.used().contains(node))) {
              for (int k = 0; k < j; k++) dist.rollback(); // rollback assignments
              continue outer;
            }

            dist.assign(op, singletonList(node));
          }

          continuation.run();

          for (int j = 0, bound1 = opEqClass.size(); j < bound1; j++) dist.rollback();
        }
      }
  }

  private List<List<Filter>> eqClassesByPredicate(List<Filter> ops, Constraints constraints) {
    final List<List<Filter>> eqClasses = new ArrayList<>(2);

    for (Filter op : ops) {
      if (op.type() != OperatorType.PlainFilter) continue;
      if (eqClasses.stream().anyMatch(it -> it.contains(op))) continue;

      final List<Filter> eqClass = findEqClass(((PlainFilter) op).predicate(), ops, constraints);
      if (eqClass.size() > 1) eqClasses.add(eqClass);
    }

    return eqClasses;
  }

  private List<List<FilterNode>> eqClassesByPredicate(Set<FilterNode> nodes) {
    return listFilter(
        it -> it.size() > 1,
        nodes.stream().collect(Collectors.groupingBy(FilterNode::expr)).values());
  }

  private List<List<Filter>> eqClassesByAttributes(List<Filter> ops, Constraints constraints) {
    final List<List<Filter>> eqClasses = new ArrayList<>(2);

    for (Filter op : ops) {
      if (eqClasses.stream().anyMatch(it -> it.contains(op))) continue;

      final List<Filter> eqClass = findEqClass(op.fields(), ops, constraints);
      if (eqClass.size() > 1) eqClasses.add(eqClass);
    }

    return eqClasses;
  }

  private List<List<FilterNode>> eqClassesByAttributes(Set<FilterNode> nodes) {
    return listFilter(
        it -> it.size() > 1,
        nodes.stream().collect(Collectors.groupingBy(FilterNode::usedAttributes)).values());
  }
}
