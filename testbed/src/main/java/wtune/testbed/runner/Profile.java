package wtune.testbed.runner;

import org.apache.commons.lang3.tuple.Pair;
import wtune.common.utils.Args;
import wtune.common.utils.IOSupport;
import wtune.common.utils.SetSupport;
import wtune.stmt.App;
import wtune.stmt.Statement;
import wtune.stmt.support.OptimizerType;
import wtune.testbed.population.Generators;
import wtune.testbed.population.PopulationConfig;
import wtune.testbed.profile.Metric;
import wtune.testbed.profile.ProfileConfig;
import wtune.testbed.util.DataSourceSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Arrays.asList;
import static wtune.sql.ast.SqlNode.MySQL;
import static wtune.testbed.profile.ProfileSupport.compare;

public class Profile implements Runner {
  public static final System.Logger LOG = System.getLogger("profile");

  private Set<String> appNames;
  private String tag;
  private Set<String> stmts;
  private String startStmt;
  private Path out;
  private boolean useSqlServer;
  private boolean dryRun;

  // Determine the optimized statement pool
  private String optimizedBy;

  private Blacklist blacklist;

  private void initBlackList() {
    blacklist = new Blacklist();
    blacklist.block(GenerateTableData.ZIPF, "redmine-307");
    blacklist.block(GenerateTableData.ZIPF, "redmine-808");
    blacklist.block(GenerateTableData.ZIPF, "redmine-942");
  }

  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    final String targetApps = args.getOptional("app", String.class, "all");
    final String targetStmts = args.getOptional("stmt", String.class, null);
    final String dir = args.getOptional("dir", String.class, "profile");

    if ("all".equals(targetApps)) this.appNames = SetSupport.map(App.all(), App::name);
    else this.appNames = new HashSet<>(asList(targetApps.split(",")));

    if (targetStmts != null) stmts = new HashSet<>(asList(targetStmts.split(",")));

    startStmt = args.getOptional("start", String.class, null);

    tag = args.getOptional("tag", String.class, GenerateTableData.BASE);
    useSqlServer = args.getOptional("sqlserver", boolean.class, true);
    dryRun = args.getOptional("dry", boolean.class, false);

    optimizedBy = args.getOptional("opt", "optimizer", String.class, "WeTune");

    final String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    final String suffix = optimizedBy + "_" + (useSqlServer ? "ss" : "pg");
    out = Runner.dataDir().resolve(dir).resolve(optimizedBy).resolve("%s_%s.%s.csv".formatted(tag, suffix, time));

    if (!Files.exists(out)) {
      Files.createDirectories(out.getParent());
      Files.createFile(out);
    }

    initBlackList();
  }

  @Override
  public void run() throws Exception {
    final List<String> failures = new ArrayList<>();

    final List<Statement> stmtPool = getStmtPool();
    boolean started = (startStmt == null);
    for (Statement stmt : stmtPool) {
      if (stmts != null && !stmts.contains(stmt.toString())) continue;
      if (appNames != null && !appNames.contains(stmt.appName())) continue;
      if (blacklist != null && blacklist.isBlocked(tag, stmt)) continue;
      if (!started) {
        if (startStmt.equals(stmt.toString())) started = true;
        else continue;
      }

      if (!runOne(stmt.original(), stmt.rewritten(OptimizerType.valueOf(optimizedBy)))) {
        LOG.log(WARNING, "failed to profile {0}", stmt.original());
        failures.add(stmt.toString());
      }
    }

    LOG.log(WARNING, "failed to profile {0}", failures);
  }

  private List<Statement> getStmtPool() {
    final OptimizerType type = OptimizerType.valueOf(optimizedBy);
    System.out.println("Optimize type: " + type);
    return Statement.findAllRewritten(type);
  }

  private boolean runOne(Statement original, Statement rewritten) {
    final PopulationConfig popConfig = GenerateTableData.mkConfig(tag);
    final ProfileConfig config = ProfileConfig.mk(Generators.make(popConfig));
    config.setDryRun(dryRun);
    config.setUseSqlServer(useSqlServer);
    config.setDbProperties(getDbProps(original.app()));
    config.setParamSaveFile(getParamSaveFile(tag));
    config.setWarmupCycles(10);
    config.setProfileCycles(100);

    LOG.log(System.Logger.Level.INFO, "start profile {0}", original);

    try{
      final Pair<Metric, Metric> comp = compare(original, rewritten, config);
      if (comp == null) {
        LOG.log(ERROR, "failed to profile {0}", original);
        return false;
      }

      final Metric metric0 = comp.getLeft(), metric1 = comp.getRight();
      LOG.log(
          System.Logger.Level.INFO,
          "{0} {1,number,#}\t{2,number,#}\t{3,number,#}",
          original,
          metric0.atPercentile(0.5),
          metric0.atPercentile(0.9),
          metric0.atPercentile(0.99));
      LOG.log(
          System.Logger.Level.INFO,
          "{0} {1,number,#}\t{2,number,#}\t{3,number,#}",
          rewritten,
          metric1.atPercentile(0.5),
          metric1.atPercentile(0.9),
          metric1.atPercentile(0.99));

      logResult(original, tag, metric0, metric1);
      return true;
    } catch (Exception e) {
      LOG.log(ERROR, "failed to profile {0}", original);
      e.printStackTrace();
      return false;
    }
  }

  private Properties getDbProps(App app) {
    final String dbName = app.name() + "_" + tag;
    if (useSqlServer) return DataSourceSupport.sqlserverProps(dbName);
    else if (MySQL.equals(app.dbType())) return DataSourceSupport.mysqlProps(dbName);
    else return DataSourceSupport.pgProps(dbName);
  }

  private void logResult(Statement stmt, String tag, Metric metric0, Metric metric1) {
    IOSupport.appendTo(
        out,
        writer -> {
          writer.printf(
              "%s;%d;%s;%d;%d;%d\n",
              stmt.appName(),
              stmt.stmtId(),
              tag + "_base",
              metric0.atPercentile(0.5),
              metric0.atPercentile(0.9),
              metric0.atPercentile(0.99));
          writer.printf(
              "%s;%d;%s;%d;%d;%d\n",
              stmt.appName(),
              stmt.stmtId(),
              tag + "_opt",
              metric1.atPercentile(0.5),
              metric1.atPercentile(0.9),
              metric1.atPercentile(0.99));
        });
  }

  private static Function<Statement, String> getParamSaveFile(String tag) {
    return stmt ->
        "wtune_data/params/%s_%s_%s".formatted(stmt, stmt.isRewritten() ? "opt" : "base", tag);
  }
}