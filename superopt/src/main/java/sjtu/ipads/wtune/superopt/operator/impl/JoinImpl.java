package sjtu.ipads.wtune.superopt.operator.impl;

import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.Join;

public abstract class JoinImpl extends BaseOperator implements Join {
  private final Placeholder left, right;

  protected JoinImpl() {
    super();
    left = newPlaceholder("c");
    right = newPlaceholder("c");
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
