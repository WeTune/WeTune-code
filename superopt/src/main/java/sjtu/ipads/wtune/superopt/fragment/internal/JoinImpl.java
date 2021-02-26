package sjtu.ipads.wtune.superopt.fragment.internal;

import sjtu.ipads.wtune.superopt.fragment.Join;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public abstract class JoinImpl extends BaseOperator implements Join {
  @Override
  public Placeholder leftFields() {
    return fragment().placeholders().getPick(this, 0);
  }

  @Override
  public Placeholder rightFields() {
    return fragment().placeholders().getPick(this, 1);
  }
}
