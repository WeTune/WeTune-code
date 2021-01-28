package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.List;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class TableGen implements ScriptNode {
  private final Table table;
  private final List<ColumnGen> columnGens;
  private final List<ConstraintGen> constraintGens;

  public TableGen(Table table) {
    this.table = table;
    this.columnGens = listMap(ColumnGen::new, table.columns());
    this.constraintGens =
        table.constraints().stream()
            .filter(it -> it.type() != null)
            .map(ConstraintGen::new)
            .collect(Collectors.toList());
  }

  @Override
  public void output(Output out) {
    out.indent()
        .println("{")
        .increaseIndent()
        .indent()
        .printf("tableName = '%s',\n", table.name())
        .indent()
        .println("columns = {")
        .increaseIndent();
    for (ColumnGen columnGen : columnGens) out.indent().accept(columnGen).println(",");
    out.decreaseIndent()
        .indent()
        .println("}, ")
        .indent()
        .println("constraints = {")
        .increaseIndent();
    for (ConstraintGen constraintGen : constraintGens)
      out.indent().accept(constraintGen).println(",");
    out.decreaseIndent().indent().println("}").decreaseIndent().indent().print("}");
  }
}
