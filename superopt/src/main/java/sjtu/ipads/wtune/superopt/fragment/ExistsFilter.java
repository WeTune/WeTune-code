package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public interface ExistsFilter extends Filter {
  @Override
  default OperatorType kind() {
    return OperatorType.EXISTS_FILTER;
  }

  @Override
  default boolean match(PlanNode node, Model m) {
    // TODO
    return false;
  }

  @Override
  default PlanNode instantiate(Model m, PlanContext ctx) {
    // TODO
    return null;
  }
}
