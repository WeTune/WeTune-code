package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.stmt.mutator.SelectItemNormalizer;
import sjtu.ipads.wtune.stmt.resolver.ParamResolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

public class StmtGen implements ScriptNode {
  private final Statement stmt;
  private final SQLGen sqlGen;
  private final int index;

  public StmtGen(Statement stmt, int index, boolean modifySelectItem) {
    this.index = index;
    stmt.retrofitStandard();
    if (modifySelectItem) stmt.mutate(SelectItemNormalizer.class);
    stmt.resolve(ParamResolver.class, true);

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
