package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Join;
import sjtu.ipads.wtune.superopt.plan.Placeholder;

public abstract class JoinImpl extends BasePlanNode implements Join {
  @Override
  public Placeholder leftFields() {
    return plan().placeholders().getPick(this, 0);
  }

  @Override
  public Placeholder rightFields() {
    return plan().placeholders().getPick(this, 1);
  }
}
