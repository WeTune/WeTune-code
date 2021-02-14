package sjtu.ipads.wtune.sqlparser.ast.constants;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.FieldDomain;
import sjtu.ipads.wtune.sqlparser.ast.internal.NodeFieldImpl;

import java.util.ArrayList;
import java.util.List;

public enum NodeType implements FieldDomain {
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

  private final List<FieldKey> fields = new ArrayList<>(5);

  public boolean isInstance(ASTNode node) {
    return node != null && node.nodeType() == this;
  }

  @Override
  public List<FieldKey> fields() {
    return fields;
  }

  @Override
  public <T, R extends T> FieldKey<R> attr(String name, Class<T> clazz) {
    final FieldKey<R> field = NodeFieldImpl.build(this, name, clazz);
    fields.add(field);
    return field;
  }
}
