package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.impl.SortImpl;
import sjtu.ipads.wtune.superopt.relational.SortKeys;

public interface Sort extends Operator {
  Abstraction<SortKeys> sortKeys();

  static Sort create() {
    return SortImpl.create();
  }
}
