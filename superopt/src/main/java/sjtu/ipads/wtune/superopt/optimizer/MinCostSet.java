package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.util.Complexity;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

class MinCostSet<K> extends AbstractSet<PlanNode> {
  private final Function<PlanNode, K> keyExtractor;
  private final Map<K, PlanNode> plans;
  private Complexity minCost;

  MinCostSet(Function<PlanNode, K> keyExtractor) {
    this.keyExtractor = keyExtractor;
    this.plans = new HashMap<>();
  }

  @Override
  public boolean add(PlanNode n) {
    final Complexity cost = Complexity.mk(n);
    final int cmp = minCost == null ? -1 : cost.compareTo(minCost);
    // the new plan is more costly, abandon it
    if (cmp > 0) return false;
    // the new plan is cheaper, abandon existing ones
    if (cmp < 0) {
      plans.clear();
      minCost = cost;
    }

    return plans.putIfAbsent(keyExtractor.apply(n), n) == null;
  }

  @Override
  public Iterator<PlanNode> iterator() {
    return plans.values().iterator();
  }

  @Override
  public int size() {
    return plans.size();
  }
}
