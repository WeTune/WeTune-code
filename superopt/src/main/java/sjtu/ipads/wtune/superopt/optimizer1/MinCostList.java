package sjtu.ipads.wtune.superopt.optimizer1;

import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.superopt.util.Complexity;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

class MinCostList extends AbstractList<PlanNode> {
  private final List<PlanNode> list = new ArrayList<>();
  private Complexity minCost;

  @Override
  public boolean add(PlanNode n) {
    final PlanComplexity cost = new PlanComplexity(n);
    final int cmp = minCost == null ? 0 : cost.compareTo(minCost);
    // the new plan is more costly, abandon it
    if (cmp > 0) return false;
    // the new plan is cheaper, abandon existing ones
    if (cmp < 0) {
      list.clear();
      minCost = cost;
    }
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
