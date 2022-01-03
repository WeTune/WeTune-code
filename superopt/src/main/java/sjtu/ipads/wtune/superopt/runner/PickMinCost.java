package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.profiler.Profiler;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.LeveledException.ignorable;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.assemblePlan;

public class PickMinCost implements Runner {
  private Path inFile, inTraceFile, outFile, outTraceFile, errFile;
  private boolean echo;
  private PrintWriter out, traceOut, err;
  private Properties dbPropsSeed;
  private Map<String, Properties> dbProps = new ConcurrentHashMap<>();
  private String app;

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    inFile = Path.of(args.getOptional("-i", String.class, "wtune_data/transformation.out"));
    inTraceFile = Path.of(args.getOptional("-o", String.class, "wtune_data/transformation.trace"));
    outFile = Path.of(args.getOptional("-t", String.class, inFile + ".opt"));
    outTraceFile = Path.of(args.getOptional("-t", String.class, inTraceFile + ".opt"));
    errFile = Path.of(args.getOptional("-e", String.class, "wtune_data/profile.err"));
    echo = args.getOptional("echo", boolean.class, true);
    app = args.getOptional("app", String.class, null);

    final String jdbcUrl = args.getOptional("dbUrl", String.class, null);
    final String username = args.getOptional("dbUser", String.class, null);
    final String password = args.getOptional("dbPasswd", String.class, null);
    final String dbType = args.getOptional("dbType", String.class, null);

    if (jdbcUrl != null) {
      dbPropsSeed = new Properties();
      dbPropsSeed.setProperty("jdbcUrl", jdbcUrl);
      dbPropsSeed.setProperty("username", username);
      dbPropsSeed.setProperty("dbType", dbType);
      if (password != null) dbPropsSeed.setProperty("password", password);
    } else {
      dbPropsSeed = null;
    }
  }

  private Properties mkDbProps(Statement stmt) {
    if (dbPropsSeed != null) {
      Properties props = dbProps.get(stmt.appName());
      if (props != null) return props;

      props = new Properties(dbPropsSeed);
      props.setProperty("jdbcUrl", dbPropsSeed.get("jdbcUrl") + stmt.appName() + "_base");
      dbProps.put(stmt.appName(), props);
      return props;

    } else return stmt.app().dbProps();
  }

  private PlanContext mkPlanSafe(Schema schema, String sql) {
    try {
      final SqlNode ast = SqlSupport.parseSql(schema.dbType(), sql);
      return assemblePlan(ast, schema);
    } catch (Throwable ex) {
      return null;
    }
  }

  private int pickMin(String stmtName, List<String> transformed) {
    final int pos = stmtName.indexOf('-');
    final String appName = stmtName.substring(0, pos);
    final int stmtId = Integer.parseInt(stmtName.substring(pos + 1));
    final Statement stmt = Statement.findOne(appName, stmtId);
    final Schema schema = stmt.app().schema("base");
    final SqlNode baseAst = SqlSupport.parseSql(stmt.app().dbType(), stmt.rawSql());
    baseAst.context().setSchema(schema);
    //    Workflow.normalize(baseAst); // TODO

    final PlanContext baseline;
    final List<PlanContext> candidates;

    try {
      baseline = assemblePlan(baseAst, schema);
      candidates = ListSupport.map((Iterable<String>) transformed, it -> mkPlanSafe(schema, it));

      for (int i = 0; i < candidates.size(); i++) {
        final PlanContext candidate = candidates.get(i);
        if (candidate == null) {
          if (echo) System.err.printf("%s\t%d\t%s\n", stmt, i, transformed.get(i));
          err.printf("%s\t%d\t%s\n", stmt, i, transformed.get(i));
        }
      }

      final Profiler profiler = Profiler.mk(mkDbProps(stmt));
      profiler.setBaseline(baseline);
      candidates.forEach(profiler::profile);

      return profiler.minCostIndex();

    } catch (Throwable ex) {
      if (!ignorable(ex)) {
        if (echo)
          System.err.println(
              stmt + " error: " + coalesce(ex.getMessage(), ex.getClass().getSimpleName()));
        err.print("> ");
        err.println(stmt);
        ex.printStackTrace(err);
      }
      return -1;
    }
  }

  @Override
  public void run() throws Exception {
    final List<String> transformations = Files.readAllLines(inFile);
    List<String> traces = Files.exists(inTraceFile) ? Files.readAllLines(inTraceFile) : null;
    if (traces != null && traces.size() != transformations.size()) traces = null;

    out = new PrintWriter(Files.newOutputStream(outFile));
    err = new PrintWriter(Files.newOutputStream(errFile));
    traceOut = traces == null ? null : new PrintWriter(Files.newOutputStream(outTraceFile));
    if (traceOut == null) System.out.println("No Trace!");

    String stmtId = null;
    List<String> group = new ArrayList<>(16);
    List<String> groupTrace = new ArrayList<>(16);

    //    final String startPoint = "redmine-1229";
    //    boolean start = "".equals(startPoint);

    for (int i = 0, bound = transformations.size(); i < bound; i++) {
      if (echo && i % 500 == 0) System.out.println(i);
      final String transformation = transformations.get(i);
      final String[] fields = transformation.split("\t", 3);

      if (app != null && !fields[0].startsWith(app)) continue;
      //      if (fields[0].equals(startPoint)) start = true;
      //      if (!start) continue;

      if (fields[0].equals(stmtId)) {
        group.add(fields[2]);
        if (traces != null) groupTrace.add(traces.get(i).split("\t", 3)[2]);
        continue;
      }

      if (stmtId != null) {
        final int minIndex = pickMin(stmtId, group);
        if (minIndex != -1) {
          out.printf("%s\t%s\n", stmtId, group.get(minIndex));
          out.flush();
          if (traces != null) {
            traceOut.printf("%s\t%s\n", stmtId, groupTrace.get(minIndex));
            traceOut.flush();
          }
        }
      }

      stmtId = fields[0];
      group.clear();
      groupTrace.clear();
      group.add(fields[2]);
      if (traces != null) groupTrace.add(traces.get(i).split("\t", 3)[2]);
    }
  }
}
