package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.relational.ConcreteColumns;
import sjtu.ipads.wtune.superopt.relational.InputSource;

public class ConcreteColumnImpl implements ConcreteColumns {
  private final Abstraction<InputSource> relation;

  private ConcreteColumnImpl(Abstraction<InputSource> relation) {
    this.relation = relation;
  }

  public static ConcreteColumnImpl create(Abstraction<InputSource> relation) {
    return new ConcreteColumnImpl(relation);
  }

  @Override
  public Abstraction<InputSource> relation() {
    return relation;
  }
}
