package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class SchemaGen implements ScriptNode {
  private final List<TableGen> children;

  public SchemaGen(Schema schema) {
    this.children = listMap(TableGen::new, schema.tables());
  }

  @Override
  public List<? extends ScriptNode> children() {
    return children;
  }

  @Override
  public void outputHead(Output out) {
    out.println("local tables = {").increaseIndent();
  }

  @Override
  public void outputTail(Output out) {
    out.decreaseIndent().println("}").println().println("return tables");
  }

  @Override
  public void outputAfterChild(Output out) {
    out.println(",");
  }
}
