package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.BaseCongruence;
import sjtu.ipads.wtune.common.utils.BaseCongruentClass;
import sjtu.ipads.wtune.sqlparser.plan.PlanContext;

class Memo extends BaseCongruence<String, SubPlan> {
  boolean isRegistered(SubPlan node) {
    return classes.containsKey(extractKey(node));
  }

  boolean isRegistered(PlanContext plan, int nodeId) {
    return classes.containsKey(extractKey(new SubPlan(plan, nodeId)));
  }

  @Override
  protected String extractKey(SubPlan subPlan) {
    return subPlan.toString();
  }

  @Override
  protected BaseCongruentClass<SubPlan> mkCongruentClass() {
    return new OptGroup(this);
  }
}
