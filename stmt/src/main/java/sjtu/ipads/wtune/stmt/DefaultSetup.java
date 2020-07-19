package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.stmt.dao.StatementDao;
import sjtu.ipads.wtune.stmt.dao.internal.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.function.Supplier;

public class DefaultSetup extends Setup {
  @Override
  public void setup() {
    final Supplier<Connection> supplier =
        StatementDao.connectionSupplier(
            "jdbc:sqlite://" + dataDir().resolve("wtune.db").toString());
    new ConstantAppDao().registerAsGlobal();
    new FileSchemaDao().registerAsGlobal();
    new DbSchemaPatchDao(supplier).registerAsGlobal();
    new DbStatementDao(supplier).registerAsGlobal();
    new DbAltStatementDao(supplier).registerAsGlobal();
    new DbTimingDao(supplier).registerAsGlobal();
    new DbFingerprintDao(supplier).registerAsGlobal();
  }

  @Override
  public Path dataDir() {
    final String wtuneDataDir = System.getProperty("wtune.dataDir");
    if (wtuneDataDir != null) return Paths.get(wtuneDataDir);
    final Path wd = Paths.get(System.getProperty("user.dir"));
    Path dataDir;
    if ((dataDir = wd.resolve("data")).toFile().exists()) return dataDir;
    if ((dataDir = wd.getParent().resolve("data")).toFile().exists()) return dataDir;
    return null;
  }

  @Override
  public Path outputDir() {
    final String wtuneScriptDir = System.getProperty("wtune.outputDir");
    if (wtuneScriptDir != null) return Paths.get(wtuneScriptDir);
    final Path wd = Paths.get(System.getProperty("user.dir"));
    Path dataDir;
    if ((dataDir = wd.resolve("wtune_out")).toFile().exists()) return dataDir;
    if ((dataDir = wd.getParent().resolve("wtune_out")).toFile().exists()) return dataDir;
    return null;
  }
}
