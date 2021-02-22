package sjtu.ipads.wtune.superopt.optimization.internal;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.listConcatView;
import static sjtu.ipads.wtune.common.utils.FuncUtils.collectionFilter;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;

public class FilterHint {
  public static Iterable<PlanNode> rearrangeFilter(
      FilterGroupNode filter, Operator op, Interpretations inter) {
    final List<SubqueryFilter> subOps = collectSubqueryFilters(op);
    final Set<FilterNode> subFilters =
        collectionFilter(it -> it instanceof SubqueryFilterNode, filter.filters(), HashSet::new);
    if (subFilters.size() != subOps.size()) return emptyList();

    final List<PlainFilter> plainOps = collectPlainFilters(op);
    final Set<FilterNode> plainFilters =
        collectionFilter(it -> it instanceof PlainFilterNode, filter.filters(), HashSet::new);
    if (plainOps.isEmpty() ^ plainFilters.isEmpty()) return emptyList();

    final List<List<PlanNode>> plainDist = distributePlainFilters(plainFilters, plainOps, inter);
    final List<List<PlanNode>> subDist = distributeSubqueryFilters(subFilters, subOps, inter);
    final PlanNode predecessor = filter.predecessors()[0];

    return Lists.cartesianProduct(plainDist, subDist).stream()
        .map(it -> listConcatView(it.get(0), it.get(1)))
        .map(it -> rebuildFilters(it, predecessor))
        .collect(Collectors.toList());
  }

  private static List<PlainFilter> collectPlainFilters(Operator op) {
    final List<PlainFilter> filters = new ArrayList<>(2);
    while (op.type().isFilter()) {
      if (op instanceof PlainFilter) filters.add((PlainFilter) op);
      op = op.predecessors()[0];
    }
    return filters;
  }

  private static List<SubqueryFilter> collectSubqueryFilters(Operator op) {
    final List<SubqueryFilter> filters = new ArrayList<>(2);
    while (op.type().isFilter()) {
      if (op instanceof SubqueryFilter) filters.add((SubqueryFilter) op);
      op = op.predecessors()[0];
    }
    return filters;
  }

  private static List<List<PlanNode>> distributePlainFilters(
      Set<FilterNode> filters, List<PlainFilter> ops, Interpretations inter) {
    return new PlainFilterDistributor(filters, ops, inter).distribute();
  }

  private static List<List<PlanNode>> distributeSubqueryFilters(
      Set<FilterNode> filters, List<SubqueryFilter> ops, Interpretations inter) {
    return new SubqueryFilterDistributor(filters, ops, inter).distribute();
  }

  private static PlanNode rebuildFilters(List<PlanNode> nodes, PlanNode predecessor) {
    assert !nodes.isEmpty();

    final PlanNode head = nodes.get(0).copy();
    PlanNode prev = head;

    for (PlanNode node : nodes.subList(1, nodes.size())) {
      final PlanNode copy = node.copy();
      prev.setPredecessor(0, copy);
      prev = copy;
    }

    prev.setPredecessor(0, predecessor.copy());

    return head;
  }

  private static FilterNode combineFilters(FilterNode f0, FilterNode f1) {
    final ASTNode binary = ASTNode.expr(ExprKind.BINARY);
    binary.set(BINARY_LEFT, f0.expr().copy());
    binary.set(BINARY_RIGHT, f1.expr().copy());
    binary.set(BINARY_OP, BinaryOp.AND);

    return PlainFilterNode.make(binary, listConcatView(f0.usedAttributes(), f1.usedAttributes()));
  }

  private abstract static class FilterDistributor<T extends Operator> {
    protected final Set<FilterNode> filters;
    protected final List<T> operators;
    protected final Set<FilterNode> used;
    protected final Interpretations inter;
    private final List<PlanNode> assigned;
    private final List<List<PlanNode>> results;

    private FilterDistributor(Set<FilterNode> filters, List<T> operators, Interpretations inter) {
      this.filters = filters;
      this.operators = operators;
      this.assigned = new ArrayList<>(operators.size());
      this.inter = inter;
      this.used = new HashSet<>();
      this.results = new ArrayList<>(operators.size() << 1);
    }

    List<List<PlanNode>> distribute() {
      distribute0(0);
      return results;
    }

    private void distribute0(int idx) {
      if (idx >= operators.size()) {
        // only if all filters are occupied, the distribution is valid
        if (used.size() == filters.size()) results.add(new ArrayList<>(assigned));
        return;
      }

      for (Set<FilterNode> assignment : makeAssignments(idx)) {
        assert !assignment.isEmpty();
        final FilterNode node = assignment.stream().reduce(FilterHint::combineFilters).get();
        assigned.add(node);
        used.addAll(assignment);

        distribute0(idx + 1);

        assigned.remove(idx);
        used.removeAll(assignment);
      }
    }

    protected abstract Iterable<Set<FilterNode>> makeAssignments(int idx);
  }

  private static class PlainFilterDistributor extends FilterDistributor<PlainFilter> {
    private PlainFilterDistributor(
        Set<FilterNode> filters, List<PlainFilter> operators, Interpretations inter) {
      super(filters, operators, inter);
    }

    private Set<FilterNode> candidatesOf(PlainFilter op) {
      final AttributeInterpretation attr = inter.getAttributes(op.fields());
      final PredicateInterpretation pred = inter.getPredicate(op.predicate());

      Set<FilterNode> candidates = filters;
      if (attr != null)
        candidates =
            collectionFilter(
                it -> attr.isCompatible(it.usedAttributes()), candidates, HashSet::new);
      if (pred != null)
        candidates = collectionFilter(it -> pred.isCompatible(it.expr()), candidates, HashSet::new);

      return candidates;
    }

    @Override
    protected Iterable<Set<FilterNode>> makeAssignments(int idx) {
      final Set<FilterNode> candidates = Sets.difference(candidatesOf(operators.get(idx)), used);
      if (idx == operators.size() - 1)
        return singletonList(candidates); // last one should occupy all the remaining

      final int max = filters.size() - used.size() - (operators.size() - idx - 1);
      assert max > 0;

      return Sets.combinations(candidates, max);
    }
  }

  private static class SubqueryFilterDistributor extends FilterDistributor<SubqueryFilter> {

    private SubqueryFilterDistributor(
        Set<FilterNode> filters, List<SubqueryFilter> operators, Interpretations inter) {
      super(filters, operators, inter);
    }

    private Set<FilterNode> candidatesOf(SubqueryFilter op) {
      final AttributeInterpretation attr = inter.getAttributes(op.fields());

      Set<FilterNode> candidates = filters;
      if (attr != null)
        candidates =
            collectionFilter(
                it -> attr.isCompatible(it.usedAttributes()), candidates, HashSet::new);

      return candidates;
    }

    @Override
    protected Iterable<Set<FilterNode>> makeAssignments(int idx) {
      return Iterables.transform(candidatesOf(operators.get(idx)), Collections::singleton);
    }
  }
}
