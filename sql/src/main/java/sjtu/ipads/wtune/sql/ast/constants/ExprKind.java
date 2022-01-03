package sjtu.ipads.wtune.sql.ast.constants;

import static sjtu.ipads.wtune.sql.ast.NodeFields.EXPR_KIND;

import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.ast.FieldDomain;
import sjtu.ipads.wtune.sql.ast.internal.ExprFieldImpl;

public enum ExprKind implements FieldDomain {
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

  private final List<FieldKey> fields = new ArrayList<>(5);

  public boolean isInstance(ASTNode node) {
    return node != null && EXPR_KIND.get(node) == this;
  }

  @Override
  public List<FieldKey> fields() {
    return fields;
  }

  @Override
  public <T, R extends T> FieldKey<R> attr(String name, Class<T> clazz) {
    final FieldKey<R> field = ExprFieldImpl.build(this, name, clazz);
    fields.add(field);
    return field;
  }
}
