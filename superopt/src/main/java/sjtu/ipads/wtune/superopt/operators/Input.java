package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.relational.InputSource;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.impl.InputImpl;

public interface Input extends Operator {
  int index();

  boolean canBeTable();

  Abstraction<InputSource> source();

  static Input create(int idx) {
    return InputImpl.create(idx);
  }
}
