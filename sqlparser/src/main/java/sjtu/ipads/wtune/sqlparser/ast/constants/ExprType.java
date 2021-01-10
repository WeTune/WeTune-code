package sjtu.ipads.wtune.sqlparser.ast.constants;

import sjtu.ipads.wtune.sqlparser.ast.AttrDomain;
import sjtu.ipads.wtune.sqlparser.ast.NodeAttrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.EXPR_ATTR_PREFIX;

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
    return node.get(NodeAttrs.EXPR_KIND) == this;
  }

  @Override
  public String attrPrefix() {
    return EXPR_ATTR_PREFIX;
  }
}
