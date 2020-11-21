package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.impl.AggImpl;

public interface Agg extends Operator {
  static Agg create() {
    return AggImpl.create();
  }
}
