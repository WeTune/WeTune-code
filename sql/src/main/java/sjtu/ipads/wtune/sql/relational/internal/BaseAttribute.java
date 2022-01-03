package sjtu.ipads.wtune.sql.relational.internal;

import sjtu.ipads.wtune.sql.relational.Attribute;
import sjtu.ipads.wtune.sql.relational.Relation;

import static sjtu.ipads.wtune.sql.util.ASTHelper.simpleName;

public abstract class BaseAttribute implements Attribute {
  private final Relation owner;
  private final String name;

  protected BaseAttribute(Relation owner, String name) {
    this.owner = owner;
    this.name = simpleName(name);
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
