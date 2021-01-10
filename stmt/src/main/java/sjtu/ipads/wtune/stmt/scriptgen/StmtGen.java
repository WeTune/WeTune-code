package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.stmt.Statement;

public class StmtGen implements ScriptNode {
  private final Statement stmt;
  private final SQLGen sqlGen;
  private final int index;

  public StmtGen(Statement stmt, int index) {
    this.index = index;
    this.stmt = stmt;
    this.sqlGen = new SQLGen(stmt.parsed());
  }

  @Override
  public void output(Output out) {
    out.println("{")
        .indent()
        .printf("index = %d, stmtId = %d,\n", index, stmt.stmtId())
        .accept(sqlGen)
        .print("}");
  }
}
