package sjtu.ipads.wtune.sqlparser.ast.constants;

import sjtu.ipads.wtune.sqlparser.ast.AttrDomain;
import sjtu.ipads.wtune.sqlparser.ast.NodeAttrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import static sjtu.ipads.wtune.sqlparser.ast.TableSourceAttrs.TABLE_SOURCE_ATTR_PREFIX;

public enum TableSourceType implements AttrDomain {
  SIMPLE,
  JOINED,
  DERIVED;

  @Override
  public String attrPrefix() {
    return TABLE_SOURCE_ATTR_PREFIX;
  }

  @Override
  public boolean isInstance(SQLNode node) {
    return node != null && node.get(NodeAttrs.TABLE_SOURCE_KIND) == this;
  }

  public boolean isJoined() {
    return this == JOINED;
  }

  public boolean isSimple() {
    return this == SIMPLE;
  }

  public boolean isDerived() {
    return this == DERIVED;
  }
}
