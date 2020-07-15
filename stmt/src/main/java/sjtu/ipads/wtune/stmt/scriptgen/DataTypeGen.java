package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.sqlparser.SQLDataType;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.surround;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class DataTypeGen implements ScriptNode {
  private final SQLDataType type;

  public DataTypeGen(SQLDataType type) {
    this.type = type;
  }

  @Override
  public void output(Output out) {
    out.print("{ ")
        .printf("category = '%s', ", type.category().name().toLowerCase())
        .printf("name = '%s', ", type.name())
        .printf("width = %d, ", type.width())
        .printf("precision = %d, ", type.precision());

    final List<String> values = type.valuesList();
    if (values != null && !values.isEmpty())
      out.printf("values = {").prints(", ", listMap(it -> surround(it, '\''), values)).print(" }");

    out.print("}");
  }
}
