package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.utils.DbUtils;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.POSTGRESQL;

public class PatchGen implements ScriptNode {
  private final SchemaPatch patch;
  private final boolean isApplication;
  private final String dbType;

  public PatchGen(SchemaPatch patch, boolean isApplication) {
    this.patch = patch;
    this.isApplication = isApplication;
    this.dbType = App.of(patch.schema()).dbType();
  }

  @Override
  public void output(Output out) {
    if (isApplication)
      out.printf(
          "CREATE INDEX %s ON %s(%s);",
          quoteName(genName(patch)),
          quoteName(patch.table()),
          String.join(", ", listMap(this::quoteName, patch.columns())));
    else if (MYSQL.equals(dbType))
      out.printf("DROP INDEX %s ON %s;", quoteName(genName(patch)), quoteName(patch.table()));
    else if (POSTGRESQL.equals(dbType))
      out.printf("DROP INDEX %s IF EXISTS;", quoteName(genName(patch)));
  }

  private String quoteName(String name) {
    return DbUtils.quoteName(name, dbType);
  }

  private static String genName(SchemaPatch patch) {
    return "wtune_idx_" + patch.table() + "_" + String.join("_", patch.columns());
  }
}
