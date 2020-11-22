package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.operators.impl.LimitImpl;

public interface Limit extends Operator {
  static Limit create() {
    return LimitImpl.create();
  }
}
