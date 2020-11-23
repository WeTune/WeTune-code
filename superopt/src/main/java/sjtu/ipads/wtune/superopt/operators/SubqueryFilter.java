package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.impl.SubqueryFilterImpl;
import sjtu.ipads.wtune.superopt.relational.SubqueryPredicate;

public interface SubqueryFilter extends Operator {
  Abstraction<SubqueryPredicate> predicate();

  @Override
  default boolean canBeQueryOut() {
    return false;
  }

  static SubqueryFilter create() {
    return SubqueryFilterImpl.create();
  }
}
