package sjtu.ipads.wtune.sqlparser.ast.constants;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.ast.AttrDomain;
import sjtu.ipads.wtune.sqlparser.ast.NodeAttr;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.internal.TableSourceAttrImpl;

public enum TableSourceType implements AttrDomain {
  SIMPLE_SOURCE,
  JOINED,
  DERIVED_SOURCE;

  @Override
  public boolean isInstance(SQLNode node) {
    return node != null && node.get(NodeAttr.TABLE_SOURCE_KIND) == this;
  }

  public boolean isJoined() {
    return this == JOINED;
  }

  public boolean isSimple() {
    return this == SIMPLE_SOURCE;
  }

  public boolean isDerived() {
    return this == DERIVED_SOURCE;
  }

  @Override
  public <T, R extends T> AttrKey<R> attr(String name, Class<T> clazz) {
    return TableSourceAttrImpl.build(this, name, clazz);
  }
}
