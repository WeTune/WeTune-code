package sjtu.ipads.wtune.sqlparser.ast.constants;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.ast.AttrDomain;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.internal.NodeAttrImpl;

public enum NodeType implements AttrDomain {
  INVALID,
  NAME_2,
  NAME_3,
  TABLE_NAME,
  COLUMN_NAME,
  CREATE_TABLE,
  ALTER_SEQUENCE,
  ALTER_TABLE,
  ALTER_TABLE_ACTION,
  COLUMN_DEF,
  REFERENCES,
  INDEX_DEF,
  KEY_PART,
  SET_OP,
  QUERY,
  QUERY_SPEC,
  SELECT_ITEM,
  EXPR,
  ORDER_ITEM,
  GROUP_ITEM,
  WINDOW_SPEC,
  WINDOW_FRAME,
  FRAME_BOUND,
  TABLE_SOURCE,
  INDEX_HINT,
  STATEMENT;

  public boolean isInstance(SQLNode node) {
    return node != null && node.nodeType() == this;
  }

  @Override
  public <T, R extends T> AttrKey<R> attr(String name, Class<T> clazz) {
    return NodeAttrImpl.build(this, name, clazz);
  }
}
