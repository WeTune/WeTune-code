package sjtu.ipads.wtune.superopt.optimization.internal;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.PlainFilter;
import sjtu.ipads.wtune.superopt.fragment.SubqueryFilter;
import sjtu.ipads.wtune.superopt.fragment.symbolic.AttributeInterpretation;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretations;
import sjtu.ipads.wtune.superopt.fragment.symbolic.PredicateInterpretation;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.Collections.*;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.plan.PlanNode.*;

public class FilterHintOld {
  private final Set<FilterNode> filters;
  private final Set<FilterNode> subqueryFilters;
  private final Interpretations inter;

  private final Set<FilterNode> used;
  private final List<FilterNode> assigned;

  private final boolean forceFullMatch;

  private final List<Pair<List<FilterNode>, Set<FilterNode>>> results;

  public FilterHintOld(Collection<FilterNode> filters, boolean forceFullMatch, Interpretations inter) {
    this.filters = new LinkedHashSet<>(filters);
    this.subqueryFilters =
        collectionFilter(it -> it instanceof SubqueryFilterNode, filters, HashSet::new);
    this.forceFullMatch = forceFullMatch;
    this.inter = inter;

    this.used = new HashSet<>();
    this.assigned = new ArrayList<>();

    this.results = new ArrayList<>();
  }

  public static Iterable<PlanNode> rearrangeFilter(
      FilterNode chainHead, Operator op, Interpretations inter) {
    final List<FilterNode> filterNodes = gatherFilters(chainHead);
    final List<Operator> filterOps = gatherFilters(op);
    final boolean forceFullMatch = op.successor() != null;

    final FilterHintOld distributor = new FilterHintOld(filterNodes, forceFullMatch, inter);

    final List<Operator> subOps = listFilter(it -> it instanceof SubqueryFilter, filterOps);
    // if SubqueryFilters in plan is insufficient, then must fail to match
    if (distributor.subqueryFilters.size() < subOps.size()) return emptyList();

    final List<Operator> plainOps = listFilter(it -> it instanceof PlainFilter, filterOps);
    // if PlainFilters in plan is insufficient, then must fail to match
    if (distributor.filters.size() - subOps.size() < plainOps.size()) return emptyList();

    // distribute nodes to operators
    distributor.distribute(listJoin(subOps, plainOps), 0);

    // rebuild filter chain
    final PlanNode predecessor = filterNodes.get(filterNodes.size() - 1).predecessors()[0];
    final List<PlanNode> rebuiltChains = new ArrayList<>(distributor.results.size());
    for (var pair : distributor.results) {
      final List<FilterNode> used = pair.getLeft();
      final Set<FilterNode> unused = Sets.difference(distributor.filters, pair.getRight());

      assert !forceFullMatch || unused.isEmpty();

      final PlanNode matchPoint = rebuildFilters(unused, normalize(used), chainHead, predecessor);
      rebuiltChains.add(matchPoint);
      resolveUsedTree(rootOf(matchPoint));
    }

    return rebuiltChains;
  }

  private void distribute(List<? extends Operator> operators, int idx) {
    if (idx >= operators.size()) {
      if (!forceFullMatch || used.size() == filters.size()) {
        results.add(Pair.of(new ArrayList<>(assigned), new HashSet<>(used)));
      }
      return;
    }

    for (Set<FilterNode> assignment : assignmentOf(operators, idx)) {
      if (assignment.isEmpty()) continue;

      final FilterNode node = assignment.stream().reduce(FilterHintOld::combineFilters).get();
      assigned.add(node);
      used.addAll(assignment);

      distribute(operators, idx + 1);

      assigned.remove(idx);
      used.removeAll(assignment);
    }
  }

  private Iterable<Set<FilterNode>> assignmentOf(List<? extends Operator> operators, int idx) {
    final Operator op = operators.get(idx);

    if (op.type() == OperatorType.SubqueryFilter)
      return Iterables.transform(candidatesOf((SubqueryFilter) op), Collections::singleton);

    if (op.type() == OperatorType.PlainFilter) {
      final Set<FilterNode> candidates = candidatesOf((PlainFilter) op);
      candidates.removeAll(used);

      if (candidates.isEmpty()) return emptySet();
      if (forceFullMatch && idx == operators.size() - 1) return singleton(candidates);

      final int max = filters.size() - used.size() - (operators.size() - idx - 1);
      assert max > 0;

      return IntStream.range(1, Math.min(max, candidates.size()) + 1)
          .mapToObj(it -> Sets.combinations(candidates, it))
          .reduce(Sets::union)
          .get();
    }

    throw new IllegalArgumentException();
  }

  private Set<FilterNode> candidatesOf(PlainFilter op) {
    final AttributeInterpretation attr = inter.getAttributes(op.fields());
    final PredicateInterpretation pred = inter.getPredicate(op.predicate());

    Set<FilterNode> candidates = filters;
    if (attr != null)
      candidates =
          collectionFilter(
              it -> attr.isCompatible(it.usedAttributes()), candidates, LinkedHashSet::new);
    if (pred != null)
      candidates =
          collectionFilter(it -> pred.isCompatible(it.expr()), candidates, LinkedHashSet::new);

    return candidates == filters ? new LinkedHashSet<>(filters) : candidates;
  }

  private Set<FilterNode> candidatesOf(SubqueryFilter op) {
    final AttributeInterpretation attr = inter.getAttributes(op.fields());
    Set<FilterNode> candidates = subqueryFilters;
    if (attr != null)
      candidates =
          collectionFilter(it -> attr.isCompatible(it.usedAttributes()), candidates, HashSet::new);
    return candidates;
  }

  private static List<Operator> gatherFilters(Operator op) {
    final List<Operator> filters = new ArrayList<>(2);
    while (op.type().isFilter()) {
      filters.add(op);
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
      Iterable<FilterNode> unused,
      Iterable<FilterNode> used,
      PlanNode oldHead,
      PlanNode predecessor) {
    final PlanNode matchPoint = rebuildFilters0(used, copyTree(predecessor));
    final PlanNode head = rebuildFilters0(unused, matchPoint);

    copyToRoot(oldHead.successor()).replacePredecessor(oldHead, head);
    return matchPoint;
  }

  private static PlanNode rebuildFilters0(Iterable<FilterNode> nodes, PlanNode predecessor) {
    final Iterator<FilterNode> iter = nodes.iterator();
    if (!iter.hasNext()) return predecessor;

    final PlanNode head = copyFilter(iter.next());

    PlanNode prev = head;
    while (iter.hasNext()) {
      final FilterNode node = iter.next();
      final PlanNode copy = copyFilter(node);
      prev.setPredecessor(0, copy);
      prev = copy;
    }
    prev.setPredecessor(0, predecessor);

    return head;
  }

  private static FilterNode combineFilters(FilterNode f0, FilterNode f1) {
    final ASTNode binary = ASTNode.expr(ExprKind.BINARY);
    binary.set(BINARY_LEFT, f0.expr()); // no need to copy(), expr() always returns a fresh AST
    binary.set(BINARY_RIGHT, f1.expr());
    binary.set(BINARY_OP, BinaryOp.AND);

    return PlainFilterNode.make(binary, listJoin(f0.usedAttributes(), f1.usedAttributes()));
  }

  private static PlanNode copyFilter(PlanNode node) {
    final PlanNode copy = node.copy();
    if (copy instanceof SubqueryFilterNode)
      copy.setPredecessor(1, copyTree(copy.predecessors()[1]));
    return copy;
  }

  private static List<FilterNode> normalize(List<FilterNode> filters) {
    // put PlainFilters before SubqueryFilters
    final int sep = indexOf(it -> it instanceof PlainFilterNode, filters);
    if (sep == -1) return filters;
    return listJoin(filters.subList(sep, filters.size()), filters.subList(0, sep));
  }
}
