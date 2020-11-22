package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.operators.impl.JoinImpl;

public interface Join extends Operator {
  @Override
  default boolean canBeQueryOut() {
    return false;
  }

  static Join create() {
    return JoinImpl.create();
  }
}
