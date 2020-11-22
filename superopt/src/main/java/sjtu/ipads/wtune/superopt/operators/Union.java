package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.operators.impl.UnionImpl;

public interface Union extends Operator {
  static Union create() {
    return UnionImpl.create();
  }
}
