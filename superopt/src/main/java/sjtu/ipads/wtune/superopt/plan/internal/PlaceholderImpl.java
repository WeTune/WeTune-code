package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.PlanNode;

class PlaceholderImpl implements Placeholder {
  private final PlanNode scope;
  private final String tag;
  private int index;

  private PlaceholderImpl(PlanNode scope, String tag, int index) {
    this.scope = scope;
    this.tag = tag;
    this.index = index;
  }

  PlaceholderImpl(PlanNode scope, String tag) {
    this(scope, tag, 0);
  }

  @Override
  public String tag() {
    return tag;
  }

  @Override
  public Placeholder copy() {
    return new PlaceholderImpl(scope, tag, index);
  }

  @Override
  public int index() {
    return index;
  }

  @Override
  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public String toString() {
    return tag() + index();
  }
}
