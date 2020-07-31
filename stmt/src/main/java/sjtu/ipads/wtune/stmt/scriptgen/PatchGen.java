package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.schema.SchemaPatch;
import sjtu.ipads.wtune.stmt.utils.StmtHelper;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class PatchGen implements ScriptNode {
  private final SchemaPatch patch;
  private final boolean isApplication;
  private final String dbType;

  public PatchGen(SchemaPatch patch, boolean isApplication) {
    this.patch = patch;
    this.isApplication = isApplication;
    this.dbType = AppContext.of(patch.app()).dbType();
  }

  @Override
  public void output(Output out) {
    if (isApplication)
      out.printf(
          "CREATE INDEX %s ON %s(%s);",
          quoteName(genName(patch)),
          quoteName(patch.tableName()),
          String.join(", ", listMap(this::quoteName, patch.columnNames())));
    else
      out.printf("DROP INDEX %s ON %s;", quoteName(genName(patch)), quoteName(patch.tableName()));
  }

  private String quoteName(String name) {
    return StmtHelper.quoteName(name, dbType);
  }

  private static String genName(SchemaPatch patch) {
    return "wtune_idx_" + patch.tableName() + "_" + String.join("_", patch.columnNames());
  }
}
