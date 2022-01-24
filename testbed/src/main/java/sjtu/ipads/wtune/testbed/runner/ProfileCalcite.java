package sjtu.ipads.wtune.testbed.runner;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.Args;
import sjtu.ipads.wtune.common.utils.IOSupport;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.testbed.population.Generators;
import sjtu.ipads.wtune.testbed.population.PopulationConfig;
import sjtu.ipads.wtune.testbed.profile.Metric;
import sjtu.ipads.wtune.testbed.profile.ProfileConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.sql.ast.SqlNode.MySQL;
import static sjtu.ipads.wtune.testbed.profile.ProfileSupport.compare;
import static sjtu.ipads.wtune.testbed.runner.GenerateTableData.BASE;
import static sjtu.ipads.wtune.testbed.util.DataSourceSupport.*;

public class ProfileCalcite implements Runner {
  public static final System.Logger LOG = System.getLogger("profile");

  private String tag;
  private Set<String> stmts;
  private Path out;
  private boolean useSqlServer;
  private boolean dryRun;
  private Blacklist blacklist;

  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    final String targetStmts = args.getOptional("stmt", String.class, null);
    final String dir = args.getOptional("dir", String.class, "wtune_data");

    if (targetStmts != null) stmts = new HashSet<>(asList(targetStmts.split(",")));

    tag = args.getOptional("tag", String.class, BASE);
    useSqlServer = args.getOptional("sqlserver", boolean.class, false);
    dryRun = args.getOptional("dry", boolean.class, false);

    final String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    final String suffix = (useSqlServer ? "ss" : "pg") + "_cal";
    out =
        Path.of(dir)
            .resolve("profile_calcite")
            .resolve("%s_%s.%s.csv".formatted(tag, suffix, time));

    if (!Files.exists(out)) Files.createFile(out);
  }

  @Override
  public void run() throws Exception {
    final List<String> failures = new ArrayList<>();

    final List<Statement> stmtPool = Statement.findAllRewrittenOfCalcite();
    for (Statement stmt : stmtPool) {
      if (stmts != null && !stmts.contains(stmt.toString())) continue;
      if (blacklist != null && blacklist.isBlocked(tag, stmt)) continue;

      Pair<Statement, Statement> originalPair =
          Statement.findOriginalPairOfCalcite(stmt.appName(), stmt.stmtId());
      if (!runTriple(originalPair.getLeft(), originalPair.getRight(), stmt.rewritten())) {
        LOG.log(WARNING, "failed to profile {0}", stmt);
        failures.add(stmt.toString());
      }
    }

    LOG.log(WARNING, "failed to profile {0}", failures);
  }

  private boolean runTriple(Statement original0, Statement original1, Statement rewritten) {
    final PopulationConfig popConfig = GenerateTableData.mkConfig(tag);
    final ProfileConfig config0 = makeProfileConfig(popConfig, original0);
    final ProfileConfig config1 = makeProfileConfig(popConfig, original1);

    LOG.log(System.Logger.Level.INFO, "start profile {0}", rewritten);

    final Pair<Metric, Metric> comp0 = compare(original0, rewritten, config0);
    final Pair<Metric, Metric> comp1 = compare(original1, rewritten, config1);
    if (comp0 == null) {
      LOG.log(ERROR, "failed to profile {0}", original0);
      return false;
    }
    if (comp1 == null) {
      LOG.log(ERROR, "failed to profile {0}", original1);
      return false;
    }

    final Metric metricOriginal0 = comp0.getLeft(), metricRewritten = comp0.getRight();
    final Metric metricOriginal1 = comp1.getLeft();
    LOG.log(
        System.Logger.Level.INFO,
        "{0} {1,number,#}\t{2,number,#}\t{3,number,#}",
        original0,
        metricOriginal0.atPercentile(0.5),
        metricOriginal0.atPercentile(0.9),
        metricOriginal0.atPercentile(0.99));
    LOG.log(
        System.Logger.Level.INFO,
        "{0} {1,number,#}\t{2,number,#}\t{3,number,#}",
        original1,
        metricOriginal1.atPercentile(0.5),
        metricOriginal1.atPercentile(0.9),
        metricOriginal1.atPercentile(0.99));
    LOG.log(
        System.Logger.Level.INFO,
        "{0} {1,number,#}\t{2,number,#}\t{3,number,#}",
        rewritten,
        metricRewritten.atPercentile(0.5),
        metricRewritten.atPercentile(0.9),
        metricRewritten.atPercentile(0.99));

    logResult(
        original0, original1, rewritten, tag, metricOriginal0, metricOriginal1, metricRewritten);
    return true;
  }

  private ProfileConfig makeProfileConfig(PopulationConfig popConfig, Statement stmt) {
    final ProfileConfig config = ProfileConfig.mk(Generators.make(popConfig));
    config.setDryRun(dryRun);
    config.setUseSqlServer(useSqlServer);
    config.setDbProperties(getDbProps(stmt.app()));
    config.setParamSaveFile(getParamSaveFile(tag));
    config.setWarmupCycles(10);
    config.setProfileCycles(100);
    return config;
  }

  private Properties getDbProps(App app) {
    final String dbName = app.name() + "_" + tag;
    if (useSqlServer) return sqlserverProps(dbName);
    else if (MySQL.equals(app.dbType())) return mysqlProps(dbName);
    else return pgProps(dbName);
  }

  private void logResult(
      Statement original0,
      Statement original1,
      Statement rewritten,
      String tag,
      Metric metric0,
      Metric metric1,
      Metric metric2) {
    IOSupport.appendTo(
        out,
        writer -> {
          writer.printf(
              "%s;%d;%s;%d;%d;%d\n",
              original0.appName(),
              original0.stmtId(),
              tag + "_base",
              metric0.atPercentile(0.5),
              metric0.atPercentile(0.9),
              metric0.atPercentile(0.99));
          writer.printf(
              "%s;%d;%s;%d;%d;%d\n",
              original1.appName(),
              original1.stmtId(),
              tag + "_base",
              metric1.atPercentile(0.5),
              metric1.atPercentile(0.9),
              metric1.atPercentile(0.99));
          writer.printf(
              "%s;%d;%s;%d;%d;%d\n",
              rewritten.appName(),
              rewritten.stmtId(),
              tag + "_opt",
              metric2.atPercentile(0.5),
              metric2.atPercentile(0.9),
              metric2.atPercentile(0.99));
        });
  }

  private static Function<Statement, String> getParamSaveFile(String tag) {
    return stmt ->
        "wtune_data/params/%s_%s_%s".formatted(stmt, stmt.isRewritten() ? "opt" : "base", tag);
  }
}
