package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AggNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SELECT_ITEM;

public class SortReducer {
  public static boolean reduceSort(PlanNode node) {
    return reduceSort0(node, significantSortOf(node));
  }

  private static Set<PlanNode> significantSortOf(PlanNode node) {
    final SortSpec sortChain = resolveSort(node);
    final Set<PlanNode> ret = Collections.newSetFromMap(new IdentityHashMap<>());
    significantSortOf0(sortChain, ret);
    return ret;
  }

  private static void significantSortOf0(SortSpec sort, Set<PlanNode> dest) {
    if (sort == null) return;
    if (sort.enforcer != null) dest.add(sort.enforcer);
    if (sort.basedOn != null) for (SortSpec base : sort.basedOn) significantSortOf0(base, dest);
  }

  private static SortSpec resolveSort(PlanNode node) {
    if (node.type() == OperatorType.Input) return null;

    final SortSpec sort0 = resolveSort(node.predecessors()[0]);
    if (node.type().isFilter()) return sort0;

    if (node.type().isJoin()) {
      final SortSpec sort1 = resolveSort(node.predecessors()[1]);
      final boolean preserve0 = sort0 != null && sort0.limited;
      final boolean preserve1 = sort1 != null && sort1.limited;

      final SortSpec[] basedOn;
      if (preserve0 && preserve1) basedOn = new SortSpec[] {sort0, sort1};
      else if (preserve0) basedOn = new SortSpec[] {sort0};
      else if (preserve1) basedOn = new SortSpec[] {sort1};
      else return null;

      return new SortSpec(null, false, basedOn);
    }

    if (node.type() == OperatorType.Proj)
      return sort0 != null
          ? sort0.limited ? sort0 : new SortSpec(null, false, sort0.basedOn)
          : null;

    if (node.type() == OperatorType.Sort)
      if (sort0 == null) return new SortSpec(node, false, null);
      else if (sort0.limited) return new SortSpec(node, false, new SortSpec[] {sort0});
      else return new SortSpec(node, false, sort0.basedOn);

    if (node.type() == OperatorType.Limit)
      return sort0 == null ? null : new SortSpec(sort0.enforcer, true, sort0.basedOn);

    if (node.type() == OperatorType.Agg)
      if (sort0 == null) return null;
      else if (isCount((AggNode) node)) return new SortSpec(null, false, sort0.basedOn);
      else return sort0;

    assert false;
    return null;
  }

  private static boolean isCount(AggNode node) {
    for (ASTNode selectItem : node.aggregations()) {
      assert SELECT_ITEM.isInstance(selectItem);
      final ASTNode expr = selectItem.get(SELECT_ITEM_EXPR);
      if ("count".equalsIgnoreCase(expr.get(AGGREGATE_NAME))) return true;
    }
    return false;
  }

  private static boolean reduceSort0(PlanNode node, Set<PlanNode> preserved) {
    boolean changed = false;
    for (PlanNode predecessor : node.predecessors()) changed |= reduceSort0(predecessor, preserved);
    if (node.type() != OperatorType.Sort || preserved.contains(node)) return changed;
    node.successor().replacePredecessor(node, node.predecessors()[0]);
    return true;
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
