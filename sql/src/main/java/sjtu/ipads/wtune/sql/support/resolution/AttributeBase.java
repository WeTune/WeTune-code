package sjtu.ipads.wtune.sql.support.resolution;

import static sjtu.ipads.wtune.sql.SqlSupport.simpleName;

abstract class AttributeBase implements Attribute {
  private final Relation owner;
  private final String name;

  protected AttributeBase(Relation owner, String name) {
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
