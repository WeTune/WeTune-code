package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

class PlaceholderImpl implements Placeholder {
  private final Operator owner;
  private final String tag;
  private int index;

  PlaceholderImpl(Operator owner, String tag, int index) {
    this.owner = owner;
    this.tag = tag;
    this.index = index;
  }

  @Override
  public String tag() {
    return tag;
  }

  @Override
  public Operator owner() {
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
