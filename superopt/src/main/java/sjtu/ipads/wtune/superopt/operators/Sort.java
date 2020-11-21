package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.impl.SortImpl;

public interface Sort extends Operator {
  static Sort create() {
    return SortImpl.create();
  }
}
