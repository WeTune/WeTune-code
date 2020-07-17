package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.stmt.mutator.SelectItemNormalizer;
import sjtu.ipads.wtune.stmt.resolver.ParamResolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

public class StmtGen implements ScriptNode {
  private final Statement stmt;
  private final SQLGen sqlGen;

  public StmtGen(Statement stmt, boolean modifySelectItem) {
    stmt.retrofitStandard();
    if (modifySelectItem) stmt.mutate(SelectItemNormalizer.class);
    stmt.resolve(ParamResolver.class);

    this.stmt = stmt;
    this.sqlGen = new SQLGen(stmt.parsed());
  }

  @Override
  public void output(Output out) {
    out.println("{").indent().printf("stmtId = %d,\n", stmt.stmtId()).accept(sqlGen).print("}");
  }
}
