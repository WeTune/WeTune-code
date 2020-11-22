package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.operators.impl.SubqueryFilterImpl;

public interface SubqueryFilter extends Operator {
  @Override
  default boolean canBeQueryOut() {
    return false;
  }

  static SubqueryFilter create() {
    return SubqueryFilterImpl.create();
  }
}
