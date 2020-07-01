package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.schema.Table;

public interface StmtAttrs {
  String ATTR_PREFIX = "stmt.attr.";

  private static String attrPrefix(String name) {
    return ATTR_PREFIX + name;
  }

  Attrs.Key<QueryScope> RESOLVED_QUERY_SCOPE =
      Attrs.key(attrPrefix("resolvedQueryScope"), QueryScope.class);
  Attrs.Key<SimpleQueryScope.Clause> RESOLVED_CLAUSE_SCOPE =
      Attrs.key(attrPrefix("resolvedClauseScope"), SimpleQueryScope.Clause.class);
  Attrs.Key<Table> RESOLVED_TABLE = Attrs.key(attrPrefix("resolvedTable"), Table.class);
  Attrs.Key<Column> RESOLVED_COLUMN = Attrs.key(attrPrefix("resolvedColumn"), Column.class);
  Attrs.Key<TableSource> RESOLVED_TABLE_SOURCE =
      Attrs.key(attrPrefix("resolvedTableSource"), TableSource.class);
  Attrs.Key<ColumnRef> RESOLVED_COLUMN_REF =
      Attrs.key(attrPrefix("resolvedColumnRef"), ColumnRef.class);
  Attrs.Key<BoolExpr> BOOL_EXPR = Attrs.key(attrPrefix("boolExpr"), BoolExpr.class);
}
