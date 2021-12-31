package sjtu.ipads.wtune.superopt.optimizer;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sqlparser.plan.AggNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.Value;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.collect.Sets.difference;
import static java.util.Collections.emptySet;
import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.common.utils.TreeScaffold.replaceGlobal;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_NAME;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;

class SortReducer {
  private Set<PlanNode> sortNodes;
  private Set<PlanNode> enforcer;

  PlanNode reduceSort(PlanNode node) {
    gatherSortEnforcer(resolveSortChain(node));

    if (sortNodes == null) return node;
    if (enforcer == null) enforcer = emptySet();

    final List<Pair<PlanNode, PlanNode>> toRemove =
        ListSupport.map(
            difference(sortNodes, enforcer),
            (Function<? super PlanNode, ? extends Pair<PlanNode, PlanNode>>)
                it -> Pair.of(it, it.predecessors()[0]));
    return toRemove.isEmpty() ? node : replaceGlobal(node, toRemove);
  }

  private void addSort(PlanNode sortNode) {
    assert sortNode.kind() == OperatorType.SORT;
    if (sortNodes == null) sortNodes = new HashSet<>();
    sortNodes.add(sortNode);
  }

  private void addEnforcer(PlanNode sortNode) {
    assert sortNode.kind() == OperatorType.SORT;
    if (enforcer == null) enforcer = new HashSet<>();
    enforcer.add(sortNode);
  }

  private SortSpec resolveSortChain(PlanNode node) {
    if (node.kind() == INPUT) return null;

    final SortSpec sort0 = resolveSortChain(node.predecessors()[0]);
    if (node.kind() == SIMPLE_FILTER) return sort0;

    if (node.kind() == IN_SUB_FILTER) {
      resolveSortChain(node.predecessors()[1]);
      return sort0;
    }

    if (node.kind().isJoin() || node.kind() == SET_OP) {
      final SortSpec sort1 = resolveSortChain(node.predecessors()[1]);
      final boolean preserve0 = sort0 != null && sort0.limited;
      final boolean preserve1 = sort1 != null && sort1.limited;

      final SortSpec[] basedOn;
      if (preserve0 && preserve1) basedOn = new SortSpec[] {sort0, sort1};
      else if (preserve0) basedOn = new SortSpec[] {sort0};
      else if (preserve1) basedOn = new SortSpec[] {sort1};
      else return null;

      return new SortSpec(null, false, basedOn);
    }

    if (node.kind() == PROJ)
      return sort0 != null
          ? sort0.limited ? sort0 : new SortSpec(null, false, sort0.basedOn)
          : null;

    if (node.kind() == LIMIT)
      return sort0 == null ? null : new SortSpec(sort0.enforcer, true, sort0.basedOn);

    if (node.kind() == AGG)
      if (sort0 == null) return null;
      else if (isCountAgg((AggNode) node)) return new SortSpec(null, false, sort0.basedOn);
      else return sort0;

    if (node.kind() == SORT) {
      addSort(node);

      if (sort0 == null) return new SortSpec(node, false, null);
      else if (sort0.limited) return new SortSpec(node, false, new SortSpec[] {sort0});
      else return new SortSpec(node, false, sort0.basedOn);
    }

    return assertFalse();
  }

  private void gatherSortEnforcer(SortSpec sort) {
    if (sort == null) return;
    if (sort.enforcer != null) addEnforcer(sort.enforcer);
    if (sort.basedOn != null) for (SortSpec base : sort.basedOn) gatherSortEnforcer(base);
  }

  private static boolean isCountAgg(AggNode node) {
    for (Value value : node.values())
      if ("count".equalsIgnoreCase(value.expr().template().get(AGGREGATE_NAME))) {
        return true;
      }
    return false;
  }

  private static class SortSpec {
    private final PlanNode enforcer;
    private final boolean limited;
    private final SortSpec[] basedOn; // at most of length 2

    private SortSpec(PlanNode enforcer, boolean limited, SortSpec[] basedOn) {
      this.enforcer = enforcer;
      this.limited = limited;
      this.basedOn = basedOn;
      assert basedOn == null || basedOn.length <= 2;
    }
  }
}
