package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

public abstract class BaseAttribute implements Attribute {
  private final Relation owner;
  private final String name;

  protected BaseAttribute(Relation owner, String name) {
    this.owner = owner;
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Relation owner() {
    return owner;
  }
}
