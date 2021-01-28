package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.sqlparser.schema.Column;

import static sjtu.ipads.wtune.sqlparser.schema.Column.Flag.IS_BOOLEAN;
import static sjtu.ipads.wtune.sqlparser.schema.Column.Flag.IS_ENUM;

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
            column.name(), column.isFlag(IS_BOOLEAN), column.isFlag(IS_ENUM))
        .print("dataType = ")
        .accept(dataTypeGen)
        .print(" }");
  }
}
