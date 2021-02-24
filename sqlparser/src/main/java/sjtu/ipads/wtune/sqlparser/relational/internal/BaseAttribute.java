package sjtu.ipads.wtune.sqlparser.relational.internal;

import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;

import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

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
