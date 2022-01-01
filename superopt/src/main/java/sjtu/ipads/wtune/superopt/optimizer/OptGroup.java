package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.BaseCongruentClass;

import java.util.Collection;

class OptGroup extends BaseCongruentClass<SubPlan> {
  protected OptGroup(Memo congruence) {
    super(congruence);
  }

  @Override
  protected Collection<SubPlan> mkCollection() {
    return new MinCostSet();
  }
}
