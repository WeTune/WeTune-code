package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.FilterNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

interface FilterChain extends List<FilterNode> {
  PlanNode successor();

  PlanNode predecessor();

  FilterNode buildChain();

  default FilterNode head() {
    return get(0);
  }

  default FilterNode tail() {
    return get(size() - 1);
  }

  static FilterChain mk(FilterNode chainHead, boolean expandCombination) {
    return FilterChainImpl.mk(chainHead, expandCombination);
  }

  static FilterChain mk(PlanNode successor, PlanNode predecessor, List<FilterNode> filters) {
    return FilterChainImpl.mk(successor, predecessor, filters);
  }
}
