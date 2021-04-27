package sjtu.ipads.wtune.testbed;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.POSTGRESQL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;
import static sjtu.ipads.wtune.testbed.population.Populator.LOG;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.LogManager;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.testbed.common.Collection;
import sjtu.ipads.wtune.testbed.population.PopulationConfig;
import sjtu.ipads.wtune.testbed.population.SQLPopulator;
import sjtu.ipads.wtune.testbed.util.DataSourceHelper;

public class PopulationMain {
  private static final String LOGGER_CONFIG =
      ".level = INFO\n"
          + "java.util.logging.ConsoleHandler.level = INFO\n"
          + "handlers=java.util.logging.ConsoleHandler\n"
          + "java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n"
          + "java.util.logging.SimpleFormatter.format=[%1$tm/%1$td %1$tT][%3$10s][%4$s] %5$s %n\n";

  static {
    try {
      LogManager.getLogManager()
          .readConfiguration(new ByteArrayInputStream(LOGGER_CONFIG.getBytes()));
    } catch (IOException ignored) {
    }
  }

  private static void populateOne(
      PopulationConfig config, String appName, String startFrom, boolean single) {
    final SQLPopulator populator = new SQLPopulator();
    populator.setConfig(config);

    final App app = App.of(appName);
    final Schema schema = app.schema("base", true);
    final List<Table> list = new ArrayList<>(schema.tables());
    list.sort(Comparator.comparing(Table::name));

    boolean start = startFrom == null;
    int populated = 0;

    LOG.log(Level.INFO, "start populating " + appName);

    final Set<String> failed = new HashSet<>();

    final long startTime = System.currentTimeMillis();
    for (Table table : list) {
      if (!start && startFrom.equals(table.name())) start = true;
      if (!start) continue;

      if (!populator.populate(Collection.ofTable(table))) failed.add(table.name());
      else ++populated;
      if (single) break;
    }
    final long endTime = System.currentTimeMillis();

    LOG.log(Level.INFO, "populated {0} tables in {1} ms", populated, endTime - startTime);

    if (!failed.isEmpty()) LOG.log(Level.WARNING, "failed to populate tables: {0}", failed);
  }

  private static Function<String, PrintWriter> fileDump(String appName, String postfix) {
    return tableName -> {
      try {
        return new PrintWriter(
            Files.newOutputStream(
                Paths.get("wtune_data/dump/%s/%s/%s.csv".formatted(appName, postfix, tableName))));

      } catch (IOException ioe) {
        throw new UncheckedIOException(ioe);
      }
    };
  }

  private static Properties dbProps(App app, String postfix) {
    final String dbType = app.dbType(), dbName = app.name() + "_" + postfix;

    final Properties dbProps;
    if (MYSQL.equals(dbType)) dbProps = DataSourceHelper.mysqlProps(dbName);
    else if (POSTGRESQL.equals(dbType)) dbProps = DataSourceHelper.pgProps(dbName);
    else throw new IllegalArgumentException("unsupported db type: " + dbType);
    return dbProps;
  }

  private static void getUsedTablesOne(Statement statement, Set<Table> dest) {
    final ASTNode ast = statement.parsed();
    ast.context().setSchema(statement.app().schema("base", true));
    ast.accept(ASTVistor.topDownVisit(it -> dest.add(it.get(RELATION).table()), SIMPLE_SOURCE));
  }

  private static Map<String, Set<Table>> getUsedTables(List<Statement> statements) {
    final Map<String, Set<Table>> tables = new HashMap<>();
    for (Statement stmt : statements) {
      getUsedTablesOne(stmt, tables.computeIfAbsent(stmt.appName(), ignored -> new HashSet<>()));
    }
    return tables;
  }

  public static void main(String[] args) {
    //    final App app = App.of("discourse");
    //    final String table = "topic_users";
    final String postfix = "large";
    //    final PopulationConfig config = PopulationConfig.make();
    //      config.setDump(fileDump(app.name(), postfix));
    //    config.setDbProperties(dbProps(app, "base"));
    //    populateOne(config, app.name(), table);
    final Map<String, Set<Table>> usedTables =
        getUsedTables(listMap(Statement::original, Statement.findAllRewritten()));

    for (var pair : usedTables.entrySet()) {
      final App app = App.of(pair.getKey());
      if (!app.name().equals("homeland")) continue;

      final PopulationConfig config = PopulationConfig.make();
      config.setDump(fileDump(app.name(), postfix));
      config.setDefaultUnitCount(100_0000);

      for (Table table : pair.getValue()) {
        populateOne(config, app.name(), table.name(), true);
      }
    }

    //    final String postfix = "large";
    //    final String startPoint = "";
    //    boolean started = startPoint.isEmpty();
    //    for (App app : App.all()) {
    //      if (!started && app.name().equals(startPoint)) started = true;
    //      if (!started) continue;

    //      final PopulationConfig config = PopulationConfig.make();
    //      config.setDump(fileDump(app.name(), postfix));
    //      config.setDbProperties(dbProps(app, postfix));
    //      config.setDryRun(true);
    //      config.setDefaultRandGen(RandomHelper.makeZipfRand(1.5));
    //      config.setDefaultUnitCount(100_0000);

    //      populateOne(config, app.name(), null);

    //      break;
    //    }
  }
}
