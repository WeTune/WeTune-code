package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.impl.DistinctImpl;

public interface Distinct extends Operator {
  static Distinct create() {
    return DistinctImpl.create();
  }
}
