package sjtu.ipads.wtune.superopt.optimizer1;

import sjtu.ipads.wtune.common.utils.BaseCongruence;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

class Memo extends BaseCongruence<String, PlanNode> {
  @Override
  protected String extractKey(PlanNode planNode) {
    return planNode.toString();
  }
}
