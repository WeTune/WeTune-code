package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.stmt.schema.Column;

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
        .printf("columnName = '%s', ", column.columnName())
        .print("dataType = ")
        .accept(dataTypeGen)
        .print(" }");
  }
}
