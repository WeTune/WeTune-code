package sjtu.ipads.wtune.stmt.schema;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN;
import static sjtu.ipads.wtune.stmt.schema.Column.*;

class ColumnBuilder {
  static Column fromColumnDef(SQLNode colDef) {
    Column column = new Column();

    colDef.put(RESOLVED_COLUMN, column);

    column.setColumnName(colDef.get(COLUMN_DEF_NAME).get(COLUMN_NAME_COLUMN));
    column.setRawDataType(colDef.get(COLUMN_DEF_DATATYPE_RAW));
    column.setDataType(colDef.get(COLUMN_DEF_DATATYPE));

    if (colDef.isFlag(COLUMN_DEF_GENERATED)) column.flag(COLUMN_GENERATED);
    if (colDef.isFlag(COLUMN_DEF_DEFAULT)) column.flag(COLUMN_HAS_DEFAULT);
    if (colDef.isFlag(COLUMN_DEF_AUTOINCREMENT)) column.flag(COLUMN_AUTOINCREMENT);

    return column;
  }
}
