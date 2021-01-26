package sjtu.ipads.wtune.sqlparser.ast.constants;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.ast.AttrDomain;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.internal.ExprAttrImpl;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.EXPR_KIND;

public enum ExprType implements AttrDomain {
  UNKNOWN,
  VARIABLE,
  COLUMN_REF,
  FUNC_CALL,
  COLLATE,
  INTERVAL,
  SYMBOL,
  LITERAL,
  PARAM_MARKER,
  AGGREGATE,
  WILDCARD,
  GROUPING_OP,
  UNARY,
  BINARY,
  TERNARY,
  TUPLE,
  EXISTS,
  MATCH,
  CAST,
  CASE,
  WHEN,
  CONVERT_USING,
  DEFAULT,
  VALUES,
  QUERY_EXPR,
  INDIRECTION,
  INDIRECTION_COMP,
  ARRAY,
  TYPE_COERCION,
  DATETIME_OVERLAP,
  COMPARISON_MOD // actually invalid, just used in parsing process
;

  public boolean isInstance(SQLNode node) {
    return EXPR_KIND.get(node) == this;
  }

  @Override
  public <T, R extends T> AttrKey<R> attr(String name, Class<T> clazz) {
    return ExprAttrImpl.build(this, name, clazz);
  }
}
