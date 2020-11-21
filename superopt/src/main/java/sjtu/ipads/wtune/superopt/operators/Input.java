package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.Relation;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.impl.InputImpl;

public interface Input extends Operator {
  int index();

  Abstraction<Relation> relation();

  static Input create(int idx) {
    return InputImpl.create(idx);
  }
}
