package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.BaseCongruence;
import sjtu.ipads.wtune.common.utils.BaseCongruentClass;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

class Memo extends BaseCongruence<String, PlanNode> {
  boolean isRegistered(PlanNode node) {
    return classes.containsKey(extractKey(node));
  }

  @Override
  protected String extractKey(PlanNode planNode) {
    return planNode.toString();
  }

  @Override
  protected BaseCongruentClass<PlanNode> mkCongruentClass() {
    return new OptGroup(this);
  }
}
