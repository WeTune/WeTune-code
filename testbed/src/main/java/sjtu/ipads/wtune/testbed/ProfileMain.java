package sjtu.ipads.wtune.testbed;

import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.testbed.population.Populator.LOG;
import static sjtu.ipads.wtune.testbed.util.DataSourceHelper.mysqlProps;
import static sjtu.ipads.wtune.testbed.util.DataSourceHelper.pgProps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.LogManager;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.testbed.population.Generators;
import sjtu.ipads.wtune.testbed.population.PopulationConfig;
import sjtu.ipads.wtune.testbed.profile.Metric;
import sjtu.ipads.wtune.testbed.profile.ProfileConfig;
import sjtu.ipads.wtune.testbed.profile.ProfileHelper;
import sjtu.ipads.wtune.testbed.util.RandomHelper;

public class ProfileMain {
  private static final String LOGGER_CONFIG =
      ".level = SEVERE\n"
          + "java.util.logging.ConsoleHandler.level = SEVERE\n"
          + "handlers=java.util.logging.ConsoleHandler\n"
          + "java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n"
          + "java.util.logging.SimpleFormatter.format=[%1$tm/%1$td %1$tT][%3$10s][%4$s] %5$s %n\n";

  private static PrintWriter out;
  private static String tag;

  private static final String BASE = "base";
  private static final String ZIPF = "zipf";
  private static final String LARGE = "large";
  private static final String LARGE_ZIPF = "large_zipf";

  static {
    try {
      LogManager.getLogManager()
          .readConfiguration(new ByteArrayInputStream(LOGGER_CONFIG.getBytes()));
    } catch (IOException ignored) {
    }
  }

  private static Function<Statement, String> paramSaveFile(String tag) {
    return stmt ->
        "wtune_data/params/%s_%s_%s".formatted(stmt, stmt.isRewritten() ? "opt" : "base", tag);
  }

  private static boolean runOne(Statement original, Statement rewritten, boolean dryRun) {
    final PopulationConfig popConfig = PopulationConfig.make();

    if (tag.equals(LARGE) || tag.equals(LARGE_ZIPF)) popConfig.setDefaultUnitCount(1_000_000);
    if (tag.equals(ZIPF) || tag.equals(LARGE_ZIPF))
      popConfig.setDefaultRandGen(() -> RandomHelper.makeZipfRand(1.5));

    final ProfileConfig config = ProfileConfig.make(Generators.make(popConfig));
    final String dbName = original.appName() + "_" + tag;
    config.setDryRun(dryRun);
    config.setDbProperties(
        MYSQL.equals(original.app().dbType()) ? mysqlProps(dbName) : pgProps(dbName));
    config.setParamSaveFile(paramSaveFile(tag));
    //    config.setWarmupCycles(20);
    //    config.setProfileCycles(201);
    config.setWarmupCycles(0);
    config.setProfileCycles(1);

    LOG.log(Level.INFO, "start profile {0}", original);

    final Pair<Metric, Metric> comp = ProfileHelper.compare(original, rewritten, config);
    if (comp == null) {
      LOG.log(Level.ERROR, "failed to profile {0}", original);
      return false;
    }

    final Metric metric0 = comp.getLeft(), metric1 = comp.getRight();
    LOG.log(
        Level.INFO,
        "{0} {1,number,#}\t{2,number,#}\t{3,number,#}",
        original,
        metric0.atPercentile(0.5),
        metric0.atPercentile(0.9),
        metric0.atPercentile(0.99));
    LOG.log(
        Level.INFO,
        "{0} {1,number,#}\t{2,number,#}\t{3,number,#}",
        rewritten,
        metric1.atPercentile(0.5),
        metric1.atPercentile(0.9),
        metric1.atPercentile(0.99));

    logResult(original, tag, metric0, metric1);
    return true;
  }

  private static final Set<String> BLACK_LIST = Set.of("solidus-33", "spree-857", "solidus-469");

  private static void run(String startPoint, boolean dryRun, boolean single) {
    boolean started = startPoint == null;

    final List<String> failed = new ArrayList<>();
    //    for (TestCase testCase : TestCases.list) {
    for (Statement rewritten : Statement.findAllRewritten()) {
      //      final Statement original = Statement.findOne(testCase.app, testCase.id);
      //      final Statement rewritten = original.rewritten();

      if (!started && rewritten.toString().equals(startPoint)) started = true;
      if (!started) continue;
      if (tag.equals(LARGE_ZIPF) && BLACK_LIST.contains(rewritten.toString())) continue;

      final Statement original = rewritten.original();

      if (!runOne(original, rewritten, dryRun)) {
        failed.add(original.toString());
      }
      if (single) break;
    }

    if (!failed.isEmpty())
      LOG.log(Level.WARNING, "failed to execute {0} statements: {1}", failed.size(), failed);
  }

  private static void logResult(Statement stmt, String tag, Metric metric0, Metric metric1) {
    if (out == null) return;
    out.printf(
        "%s;%d;%s;%d;%d;%d\n",
        stmt.appName(),
        stmt.stmtId(),
        tag + "_base",
        metric0.atPercentile(0.5),
        metric0.atPercentile(0.9),
        metric0.atPercentile(0.99));
    out.printf(
        "%s;%d;%s;%d;%d;%d\n",
        stmt.appName(),
        stmt.stmtId(),
        tag + "_opt",
        metric1.atPercentile(0.5),
        metric1.atPercentile(0.9),
        metric1.atPercentile(0.99));
    out.flush();
  }

  public static void main(String[] args) throws IOException {
    out = new PrintWriter(Files.newOutputStream(Paths.get("wtune_data/profile.csv")));
    tag = BASE;
    //    run("discourse-3842", false, true);
    run(null, false, false);
  }
}
