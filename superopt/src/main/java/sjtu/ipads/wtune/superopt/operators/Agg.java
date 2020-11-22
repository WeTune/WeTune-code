package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.impl.AggImpl;
import sjtu.ipads.wtune.superopt.relational.AggFuncs;
import sjtu.ipads.wtune.superopt.relational.GroupKeys;

public interface Agg extends Operator {
  static Agg create() {
    return AggImpl.create();
  }
  Abstraction<GroupKeys> groupKeys();
  Abstraction<AggFuncs> aggFuncs();
}
