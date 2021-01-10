package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class SQLGen implements ScriptNode {
  private final SQLNode node;
  private final String parameterizedSql;
  private final List<ParamGen> paramGens;

  public SQLGen(SQLNode node) {
    final ParameterizedSQLFormatter formatter = new ParameterizedSQLFormatter();
    node.accept(formatter);

    this.node = node;
    this.parameterizedSql = formatter.toString();
    this.paramGens = listMap(ParamGen::new, formatter.params());
  }

  @Override
  public void output(Output out) {
    out.indent()
        .println("sql = [[")
        .increaseIndent()
        .indent()
        .println(parameterizedSql.replaceAll("(?<=\n)", out.currentIndent()))
        .decreaseIndent()
        .indent()
        .println("]],");

    out.indent().println("params = {").increaseIndent();
    for (ParamGen paramGen : paramGens) out.indent().accept(paramGen).println(",");
    out.decreaseIndent().indent().println("}");
  }
}
