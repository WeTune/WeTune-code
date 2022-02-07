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
    final String dir = args.getOptional("dir", String.class, "profile_calcite");

    if (targetStmts != null) stmts = new HashSet<>(asList(targetStmts.split(",")));

    tag = args.getOptional("tag", String.class, BASE);
    useSqlServer = args.getOptional("sqlserver", boolean.class, true);
    dryRun = args.getOptional("dry", boolean.class, false);

    final String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    final String suffix = (useSqlServer ? "ss" : "pg") + "_cal";
    out = Runner.dataDir().resolve(dir).resolve("%s_%s.%s.csv".formatted(tag, suffix, time));

    if (!Files.exists(out)) {
      Files.createDirectories(out.getParent());
      Files.createFile(out);
    }
  }

  @Override
  public void run() throws Exception {
    final List<String> failuresCalcite = new ArrayList<>();
    final List<String> failuresWeTune = new ArrayList<>();

    final List<Statement> stmtPool = Statement.findAllCalcite();
    for (Statement original : stmtPool) {
      if (stmts != null && !stmts.contains(original.toString())) continue;
      if (blacklist != null && blacklist.isBlocked(tag, original)) continue;

      final Statement rewrittenCalcite = original.calciteVersion();
      final Statement rewrittenWeTune = original.rewritten();
      if (rewrittenCalcite != null && !runPair(original, rewrittenCalcite, true)) {
        LOG.log(WARNING, "failed to profile {0} with its calcite rewritten version", original);
        failuresCalcite.add(original.toString());
      }
      if (rewrittenWeTune != null && !runPair(original, rewrittenWeTune, false)) {
        LOG.log(WARNING, "failed to profile {0} with its wetune rewritten version", original);
        failuresWeTune.add(original.toString());
      }
    }
    LOG.log(WARNING, "failed to profile {0} with its calcite rewritten version", failuresCalcite);
    LOG.log(WARNING, "failed to profile {0} with its wetune rewritten version", failuresWeTune);
  }

  private boolean runPair(Statement original, Statement rewritten, boolean calciteRewrite) {
    final PopulationConfig popConfig = GenerateTableData.mkConfig(tag);
    final ProfileConfig config = makeProfileConfig(popConfig, original);

    LOG.log(
        System.Logger.Level.INFO,
        "start profile {0} {1}",
        rewritten,
        calciteRewrite ? "of calcite version" : "");

    try {
      final Pair<Metric, Metric> comp = compare(original, rewritten, config);
      if (comp == null) {
        return false;
      }

      final Metric metricOriginal = comp.getLeft(), metricRewritten = comp.getRight();
      LOG.log(
          System.Logger.Level.INFO,
          "{0} {1,number,#}\t{2,number,#}\t{3,number,#}",
          original,
          metricOriginal.atPercentile(0.5),
          metricOriginal.atPercentile(0.9),
          metricOriginal.atPercentile(0.99));
      LOG.log(
          System.Logger.Level.INFO,
          "{0} {1,number,#}\t{2,number,#}\t{3,number,#}",
          rewritten,
          metricRewritten.atPercentile(0.5),
          metricRewritten.atPercentile(0.9),
          metricRewritten.atPercentile(0.99));

      logResult(original, tag, calciteRewrite, metricOriginal, metricRewritten);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
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
      Statement stmt, String tag, boolean calciteVersion, Metric metric0, Metric metric1) {
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
              tag + (calciteVersion ? "_cal" : "_opt"),
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
