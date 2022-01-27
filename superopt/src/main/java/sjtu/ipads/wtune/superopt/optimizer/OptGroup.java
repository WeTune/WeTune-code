package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.common.utils.BaseCongruentClass;
import sjtu.ipads.wtune.common.utils.BaseNaturalCongruence;

import java.util.Collection;
import java.util.Set;

class OptGroup extends BaseCongruentClass<SubPlan> {
  protected OptGroup(Memo congruence) {
    super(congruence);
  }

  @Override
  protected void merge(BaseCongruentClass<SubPlan> other) {
    super.merge(other);
    final Memo memo = (Memo) this.congruence;
    for (String key : ((OptGroup) other).evicted()) {
      ((OptGroup) memo.eqClassAt(key)).elements = elements;
    }
  }

  private Set<String> evicted() {
    return ((MinCostSet) elements).evicted();
  }

  @Override
  protected Collection<SubPlan> mkCollection() {
    return new MinCostSet();
  }
}
