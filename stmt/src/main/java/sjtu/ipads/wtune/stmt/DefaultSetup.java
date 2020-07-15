package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.stmt.dao.StatementDao;
import sjtu.ipads.wtune.stmt.dao.internal.ConstantAppDao;
import sjtu.ipads.wtune.stmt.dao.internal.DbStatementDao;
import sjtu.ipads.wtune.stmt.dao.internal.FileSchemaDao;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DefaultSetup extends Setup {
  @Override
  public void setup() {
    new ConstantAppDao().registerAsGlobal();
    new FileSchemaDao().registerAsGlobal();
    new DbStatementDao(
            StatementDao.connectionSupplier(
                "jdbc:sqlite://" + dataDir().resolve("wtune.db").toString()))
        .registerAsGlobal();
  }

  @Override
  public Path dataDir() {
    final String wtuneDataDir = System.getProperty("wtune.dataDir");
    if (wtuneDataDir != null) return Paths.get(wtuneDataDir);
    return Paths.get(System.getProperty("user.dir")).getParent().resolve("data");
  }

  @Override
  public Path scriptOutputDir() {
    final String wtuneScriptDir = System.getProperty("wtune.scriptOutputDir");
    if (wtuneScriptDir != null) return Paths.get(wtuneScriptDir);
    return Paths.get(System.getProperty("user.dir")).getParent().resolve("script_out");
  }
}
