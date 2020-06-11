package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.schema.Table;

public class StandardAttrs {
  private static final String ATTR_PREFIX = "stmt.attr";
  public static final Attrs.Key<Table> RESOLVED_TABLE =
      Attrs.key(ATTR_PREFIX + ".resolvedTable", Table.class);
  public static final Attrs.Key<Column> RESOLVED_COLUMN =
      Attrs.key(ATTR_PREFIX + ".resolvedColumn", Column.class);
}
