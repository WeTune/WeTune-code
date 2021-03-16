package sjtu.ipads.wtune.superopt.optimizer.support;

import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.head;
import static sjtu.ipads.wtune.superopt.util.CostEstimator.compareCost;

public class MinCostList extends AbstractList<PlanNode> {
  private final List<PlanNode> list = new ArrayList<>();

  @Override
  public boolean add(PlanNode n) {
    final PlanNode currentMin = head(list);
    final int cmp = currentMin == null ? 0 : compareCost(n, currentMin);
    // the new plan is more costly, abandon it
    if (cmp > 0) return false;
    // the new plan is cheaper, abandon existing ones
    if (cmp < 0) list.clear();
    return list.add(n);
  }

  @Override
  public PlanNode get(int index) {
    return list.get(index);
  }

  @Override
  public int size() {
    return list.size();
  }
}
