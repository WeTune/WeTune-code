package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Join;
import sjtu.ipads.wtune.superopt.plan.Placeholder;

public abstract class JoinImpl extends BasePlanNode implements Join {
  private final Placeholder left, right;

  protected JoinImpl() {
    super();
    left = makePlaceholder("c");
    right = makePlaceholder("c");
  }

  @Override
  public Placeholder leftFields() {
    return left;
  }

  @Override
  public Placeholder rightFields() {
    return right;
  }
}
