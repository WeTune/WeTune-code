package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.BaseCongruentClass;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.Collection;

class OptGroup extends BaseCongruentClass<PlanNode> {
  protected OptGroup(Memo congruence) {
    super(congruence);
  }

  @Override
  protected Collection<PlanNode> mkCollection() {
    return new MinCostSet<>(((Memo) congruence)::extractKey);
  }
}
