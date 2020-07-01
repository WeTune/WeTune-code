package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.SelectItem;
import sjtu.ipads.wtune.stmt.schema.Column;

import java.util.Objects;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.SQLNode.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.SQLNode.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

public class ReplaceColumnRef implements Operator, SQLVisitor {
  private final ColumnRef target;
  private final String replacementTable;
  private final String replacementColumn;

  public ReplaceColumnRef(ColumnRef target, ColumnRef replacement) {
    this.target = target;
    this.replacementTable = replacement.source().name();

    final Column column = replacement.refColumn();
    final SelectItem item = replacement.refItem();
    this.replacementColumn =
        column != null
            ? column.columnName()
            : item.alias() != null ? item.alias() : item.simpleName();

    assert this.replacementColumn != null;
  }

  @Override
  public boolean enterColumnRef(SQLNode columnRef) {
    final ColumnRef cRef = columnRef.get(RESOLVED_COLUMN_REF);
    if (!Objects.equals(cRef, target)) return false;
    final SQLNode columnName = columnRef.get(COLUMN_REF_COLUMN);
    columnName.put(COLUMN_NAME_TABLE, replacementTable);
    columnName.put(COLUMN_NAME_COLUMN, replacementColumn);
    return false;
  }

  @Override
  public SQLNode apply(SQLNode sqlNode) {
    sqlNode.accept(this);
    return sqlNode;
  }
}
