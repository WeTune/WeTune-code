package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.stmt.schema.Column;

import static sjtu.ipads.wtune.stmt.schema.Column.COLUMN_IS_BOOLEAN;
import static sjtu.ipads.wtune.stmt.schema.Column.COLUMN_IS_ENUM;

public class ColumnGen implements ScriptNode {
  private final Column column;
  private final DataTypeGen dataTypeGen;

  public ColumnGen(Column column) {
    this.column = column;
    this.dataTypeGen = new DataTypeGen(column.dataType());
  }

  @Override
  public void output(Output out) {
    out.print("{ ")
        .printf(
            "columnName = '%s', isBoolean = %s, isEnum = %s, ",
            column.columnName(), column.isFlag(COLUMN_IS_BOOLEAN), column.isFlag(COLUMN_IS_ENUM))
        .print("dataType = ")
        .accept(dataTypeGen)
        .print(" }");
  }
}
