package sjtu.ipads.wtune.superopt.optimizer.filter;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.copyOnTree;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.copyToRoot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import sjtu.ipads.wtune.common.utils.FuncUtils;
import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.Filter;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;

public class FilterHint {

  public static Iterable<PlanNode> rearrangeFilter(
      FilterNode chainHead, Operator op, Interpretations inter) {
    final List<FilterNode> pool = gatherFilters(chainHead);
    final List<Filter> slots = gatherFilters(op);
    final boolean forceFullMatch = op.successor() != null;

    final FilterDistribution dist = new FilterDistribution(pool, slots, forceFullMatch, inter);
    if (!dist.isSatisfiable()) return emptyList();

    final FilterDistributor distributor = makeDistributor();
    distributor.distribute(dist);

    final PlanNode predecessor = tail(pool).predecessors()[0];
    final List<PlanNode> ret = new ArrayList<>(dist.results().size());
    for (List<FilterAssignment> result : dist.results()) {
      final PlanNode matchPoint = rebuildFilters(result, slots, chainHead, predecessor);
      //      resolveUsedOnTree(rootOf(matchPoint));
      ret.add(matchPoint);
    }

    return ret;
  }

  private static FilterDistributor makeDistributor() {
    final FilterDistributor distributor0 = new DecidedFilterDistributor();
    final FilterDistributor distributor1 = new BoundedFilterDistributor();
    final FilterDistributor distributor2 = new ProactiveFilterDistributor();
    final FilterDistributor distributor3 = new FreeFilterDistributor();
    distributor0.setNext(distributor1);
    distributor1.setNext(distributor2);
    distributor2.setNext(distributor3);

    return distributor0;
  }

  private static List<Filter> gatherFilters(Operator op) {
    final List<Filter> filters = new ArrayList<>(2);
    while (op.type().isFilter()) {
      filters.add((Filter) op);
      op = op.predecessors()[0];
    }
    return filters;
  }

  private static List<FilterNode> gatherFilters(PlanNode node) {
    final List<FilterNode> filters = new ArrayList<>(2);
    while (node.type().isFilter()) {
      filters.add((FilterNode) node);
      node = node.predecessors()[0];
    }
    return filters;
  }

  private static PlanNode rebuildFilters(
      List<FilterAssignment> assignments,
      Iterable<Filter> ops,
      PlanNode oldHead,
      PlanNode predecessor) {
    final FilterAssignment tail = tail(assignments);
    // unused nodes, they are shifted before the match point
    if (tail.op() == null) assignments = assignments.subList(0, assignments.size() - 1);

    assignments = sortAssignments(assignments, ops);
    final PlanNode matchPoint =
        rebuildFilters0(
            listMap(assignments, FilterAssignment::assignment), copyOnTree(predecessor));

    final PlanNode newHead;
    if (tail.op() == null) {
      newHead = tail.assignment();
      newHead.setPredecessor(0, matchPoint);
    } else newHead = matchPoint;

    copyToRoot(oldHead.successor()).replacePredecessor(oldHead, newHead);
    return matchPoint;
  }

  private static List<FilterAssignment> sortAssignments(
      List<FilterAssignment> assignments, Iterable<Filter> ops) {
    final List<FilterAssignment> ret = new ArrayList<>(assignments.size());
    for (Operator op : ops) ret.add(FuncUtils.find(assignments, it -> it.op() == op));
    assert ret.stream().noneMatch(Objects::isNull);
    return ret;
  }

  private static PlanNode rebuildFilters0(Iterable<FilterNode> nodes, PlanNode predecessor) {
    final Iterator<FilterNode> iter = nodes.iterator();
    if (!iter.hasNext()) return predecessor;

    final PlanNode head = iter.next();

    PlanNode prev = head;
    while (iter.hasNext()) {
      final FilterNode node = iter.next();
      prev.setPredecessor(0, node);
      prev = node;
    }
    prev.setPredecessor(0, predecessor);

    return head;
  }
}
