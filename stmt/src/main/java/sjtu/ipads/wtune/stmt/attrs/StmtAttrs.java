package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.schema.Table;

public class StmtAttrs {
  public static final String ATTR_PREFIX = "stmt.attr.";

  private static String attrPrefix(String name) {
    return ATTR_PREFIX + name;
  }

  public static final Attrs.Key<QueryScope> RESOLVED_QUERY_SCOPE =
      Attrs.key(attrPrefix("queryScope"), QueryScope.class);
  public static final Attrs.Key<Table> RESOLVED_TABLE =
      Attrs.key(attrPrefix("resolvedTable"), Table.class);
  public static final Attrs.Key<Column> RESOLVED_COLUMN =
      Attrs.key(attrPrefix("resolvedColumn"), Column.class);
}
