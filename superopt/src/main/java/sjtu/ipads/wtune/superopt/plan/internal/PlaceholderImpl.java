package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.PlanNode;

class PlaceholderImpl implements Placeholder {
  private final PlanNode owner;
  private final String tag;
  private int index;

  PlaceholderImpl(PlanNode owner, String tag, int index) {
    this.owner = owner;
    this.tag = tag;
    this.index = index;
  }

  @Override
  public String tag() {
    return tag;
  }

  @Override
  public PlanNode owner() {
    return owner;
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
    return "@" + tag();
  }
}
