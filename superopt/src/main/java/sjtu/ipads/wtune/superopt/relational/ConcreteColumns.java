package sjtu.ipads.wtune.superopt.relational;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.relational.impl.ConcreteColumnImpl;

public interface ConcreteColumns {
  Abstraction<Relation> relation();

  static ConcreteColumns create(Abstraction<Relation> relation) {
    return ConcreteColumnImpl.create(relation);
  }
}
