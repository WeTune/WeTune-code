package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.relational.Relation;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.operators.impl.InputImpl;

public interface Input extends Operator {
  int index();

  boolean canBeTable();

  Abstraction<Relation> relation();

  static Input create(int idx) {
    return InputImpl.create(idx);
  }
}
