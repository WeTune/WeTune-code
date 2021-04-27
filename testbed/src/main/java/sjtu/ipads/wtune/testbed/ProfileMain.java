package sjtu.ipads.wtune.testbed;

import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.testbed.population.Populator.LOG;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.testbed.population.Generators;
import sjtu.ipads.wtune.testbed.population.PopulationConfig;
import sjtu.ipads.wtune.testbed.profile.Metric;
import sjtu.ipads.wtune.testbed.profile.ProfileConfig;
import sjtu.ipads.wtune.testbed.profile.ProfileHelper;
import sjtu.ipads.wtune.testbed.util.DataSourceHelper;

public class ProfileMain {
  private static final String LOGGER_CONFIG =
      ".level = INFO\n"
          + "java.util.logging.ConsoleHandler.level = INFO\n"
          + "handlers=java.util.logging.ConsoleHandler\n"
          + "java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n"
          + "java.util.logging.SimpleFormatter.format=[%1$tm/%1$td %1$tT][%3$10s][%4$s] %5$s %n\n";

  private static PrintWriter out;
  private static String tag;

  static {
    try {
      LogManager.getLogManager()
          .readConfiguration(new ByteArrayInputStream(LOGGER_CONFIG.getBytes()));
    } catch (IOException ignored) {
    }
  }

  private static boolean runOne(Statement original, Statement rewritten) {
    final PopulationConfig popConfig = PopulationConfig.make();
    final Generators generators = Generators.make(popConfig);
    final ProfileConfig config = ProfileConfig.make(generators);
    final String dbName = original.appName() + "_base";
    config.setDbProperties(
        MYSQL.equals(original.app().dbType())
            ? DataSourceHelper.mysqlProps(dbName)
            : DataSourceHelper.pgProps(dbName));

    config.setWarmupCycles(20);
    config.setProfileCycles(201);

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

  private static void run(String startPoint, boolean single) {
    boolean started = startPoint == null;

    final List<String> failed = new ArrayList<>();
    //    for (Xxx xxx : Temp.set) {
    for (Statement rewritten : Statement.findAllRewritten()) {
      //      final Statement original = Statement.findOne(xxx.app, xxx.id);
      //      final Statement rewritten = original.rewritten();

      if (!started && rewritten.toString().equals(startPoint)) started = true;
      if (!started) continue;

      final Statement original = rewritten.original();

      if (!runOne(original, rewritten)) {
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
    //    out = new PrintWriter(Files.newOutputStream(Paths.get("wtune_data/profile.csv")));
    tag = "uniform";
    run("solidus-162", true);
    //    run(null);
  }
}
