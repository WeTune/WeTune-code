package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.SELECT_ITEM_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SELECT_ITEM;

public class ProjectionAttribute implements Attribute {
  private final SQLNode projection;

  private ProjectionAttribute(SQLNode projection) {
    this.projection = projection;
  }

  public static Attribute build(SQLNode projection) {
    if (!SELECT_ITEM.isInstance(projection)) throw new IllegalArgumentException();

    return new ProjectionAttribute(projection);
  }

  @Override
  public String name() {
    return projection.get(SELECT_ITEM_ALIAS);
  }
}
