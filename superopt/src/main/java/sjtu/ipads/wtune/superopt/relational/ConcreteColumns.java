package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.relational.impl.ConcreteColumnImpl;

public interface ConcreteColumns {
  Abstraction<InputSource> relation();

  static ConcreteColumns create(Abstraction<InputSource> relation) {
    return ConcreteColumnImpl.create(relation);
  }
}
