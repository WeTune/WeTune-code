package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.impl.PlainFilterImpl;
import sjtu.ipads.wtune.superopt.relational.PlainPredicate;

public interface PlainFilter extends Operator {
  Abstraction<PlainPredicate> predicate();

  @Override
  default boolean canBeQueryOut() {
    return false;
  }

  static PlainFilter create() {
    return PlainFilterImpl.create();
  }
}
