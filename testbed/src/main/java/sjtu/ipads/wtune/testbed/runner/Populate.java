package sjtu.ipads.wtune.testbed.runner;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.sql.schema.Table;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.testbed.common.Collection;
import sjtu.ipads.wtune.testbed.population.PopulationConfig;
import sjtu.ipads.wtune.testbed.population.SQLPopulator;
import sjtu.ipads.wtune.testbed.util.RandomHelper;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static java.lang.System.Logger.Level.*;
import static sjtu.ipads.wtune.common.utils.ArraySupport.linearFind;
import static sjtu.ipads.wtune.common.utils.ArraySupport.map;
import static sjtu.ipads.wtune.common.utils.IOSupport.io;
import static sjtu.ipads.wtune.common.utils.IOSupport.newPrintWriter;
import static sjtu.ipads.wtune.sql.SqlSupport.parseSql;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.TableName_Table;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.Simple_Table;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.SimpleSource;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.nodeLocator;

class Populate implements Runner {
  private static final System.Logger LOG = System.getLogger("population");

   static final String BASE = "base";
   static final String ZIPF = "zipf";
   static final String LARGE = "large";
   static final String LARGE_ZIPF = "large_zipf";

  private String[] appNames;
  private String tag;
  private String[] tables;
  private Path parentDir;

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    final String targetTag = args.getOptional("tag", String.class, BASE);
    final String targetApps = args.getOptional("app", String.class, "all");
    final String targetTables = args.getOptional("tables", String.class, null);
    final String parentDirPath = args.getOptional("dir", String.class, "wtune_data");

    parentDir = Path.of(parentDirPath).resolve("dump");
    if (!Files.exists(parentDir)) Files.createDirectories(parentDir);

    if ("all".equals(targetApps)) this.appNames = map(App.all(), App::name, String.class);
    else this.appNames = targetApps.split(",");

    tag = targetTag;

    if (targetTables != null && appNames.length > 0) {
      LOG.log(ERROR, "only one app is allow -tables is designated");
      throw new IllegalArgumentException();
    }
    tables = targetTables == null ? null : targetTables.split(",");
  }

  @Override
  public void run() throws Exception {
    final Map<String, Set<Table>> usedTables =
        getUsedTables(ListSupport.map(Statement.findAllRewrittenByBagSem(), Statement::original));

    for (String appName : appNames) {
      final App app = App.of(appName);
      if (app == null) {
        LOG.log(WARNING, "unknown app {0}", appName);
        continue;
      }

      final PopulationConfig config = mkConfig(appName, tag);

      final List<String> failed = new ArrayList<>();
      for (Table table : usedTables.get(appName))
        if (tables == null || linearFind(tables, table.name(), 0) > 0)
          if (!populateOne(config, app.name(), table.name())) {
            failed.add(table.name());
          }

      if (!failed.isEmpty())
        LOG.log(WARNING, "failed to populate tables for {0}: {1}", appNames, failed);
    }
  }

  private static Map<String, Set<Table>> getUsedTables(List<Statement> statements) {
    final Map<String, Set<Table>> tables = new HashMap<>();
    for (Statement stmt : statements) {
      final Set<Table> usedTables =
          tables.computeIfAbsent(stmt.appName(), ignored -> new HashSet<>());

      final SqlNode ast = parseSql(stmt.app().dbType(), stmt.rawSql());
      if (ast == null) continue;

      final Schema schema = stmt.app().schema("base");
      final SqlNodes tableSources = nodeLocator().accept(SimpleSource).gather(ast);
      for (SqlNode tableSource : tableSources) {
        final String tableName = tableSource.$(Simple_Table).$(TableName_Table);
        final Table table = schema.table(tableName);
        if (table != null) usedTables.add(table);
      }
    }

    return tables;
  }

  static PopulationConfig mkConfig(String tag) {
    final PopulationConfig config = PopulationConfig.mk();

    if (tag.equals(LARGE) || tag.equals(LARGE_ZIPF)) config.setDefaultUnitCount(1_000_000);
    else config.setDefaultUnitCount(10_000);

    if (tag.equals(ZIPF) || tag.equals(LARGE_ZIPF))
      config.setDefaultRandGen(() -> RandomHelper.makeZipfRand(1.5));
    else config.setDefaultRandGen(RandomHelper::makeUniformRand);

    return config;
  }

  private PopulationConfig mkConfig(String appName, String tag) throws IOException {
    final PopulationConfig config = mkConfig(tag);
    config.setDump(fileDump(appName, tag));
    return config;
  }

  private static boolean populateOne(PopulationConfig config, String appName, String tableName) {
    final SQLPopulator populator = new SQLPopulator();
    populator.setConfig(config);

    final App app = App.of(appName);
    final Schema schema = app.schema("base", true);
    final Table table = schema.table(tableName);

    LOG.log(INFO, "start populating {0}.{1}", appName, table);

    final long start = System.currentTimeMillis();
    final boolean success = populator.populate(Collection.ofTable(table));
    final long end = System.currentTimeMillis();

    if (success) LOG.log(TRACE, "populated {0}.{1} in {2} ms", appName, tableName, end - start);

    return success;
  }

  @SuppressWarnings("all")
  private Function<String, PrintWriter> fileDump(String appName, String postfix)
      throws IOException {
    final Path baseDir = parentDir.resolve(postfix).resolve(appName);
    if (!Files.exists(baseDir)) Files.createDirectories(baseDir);
    return tableName -> io(() -> newPrintWriter(baseDir.resolve(tableName + ".csv")));
  }
}
