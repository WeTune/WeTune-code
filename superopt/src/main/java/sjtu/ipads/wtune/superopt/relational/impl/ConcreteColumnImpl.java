package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.relational.ConcreteColumns;
import sjtu.ipads.wtune.superopt.relational.Relation;

public class ConcreteColumnImpl implements ConcreteColumns {
  private final Abstraction<Relation> relation;

  private ConcreteColumnImpl(Abstraction<Relation> relation) {
    this.relation = relation;
  }

  public static ConcreteColumnImpl create(Abstraction<Relation> relation) {
    return new ConcreteColumnImpl(relation);
  }

  @Override
  public Abstraction<Relation> relation() {
    return relation;
  }
}
