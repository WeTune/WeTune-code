package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.impl.PlainFilterImpl;

public interface PlainFilter extends Operator {
  @Override
  default boolean canBeQueryOut() {
    return false;
  }

  static PlainFilter create() {
    return PlainFilterImpl.create();
  }
}
