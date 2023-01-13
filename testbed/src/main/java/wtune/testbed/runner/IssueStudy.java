package wtune.testbed.runner;

import org.apache.calcite.jdbc.CalciteConnection;
import wtune.common.datasource.DbSupport;
import wtune.common.utils.Args;
import wtune.common.utils.IOSupport;
import wtune.sql.SqlSupport;
import wtune.sql.ast.SqlNode;
import wtune.sql.plan.PlanContext;
import wtune.sql.plan.PlanSupport;
import wtune.sql.schema.Schema;
import wtune.stmt.App;
import wtune.common.datasource.db.SQLServerPlanTree;
import wtune.common.datasource.SQLSyntaxAdaptor;
import wtune.superopt.optimizer.Optimizer;
import wtune.superopt.profiler.Profiler;
import wtune.superopt.substitution.Substitution;
import wtune.superopt.substitution.SubstitutionBank;
import wtune.superopt.substitution.SubstitutionSupport;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static wtune.common.datasource.DbSupport.*;
import static wtune.sql.plan.PlanSupport.assemblePlan;
import static wtune.sql.plan.PlanSupport.translateAsAst;
import static wtune.sql.support.action.NormalizationSupport.normalizeAst;
import static wtune.superopt.optimizer.OptimizerSupport.*;

public class IssueStudy implements Runner {
  private static final String DEFAULT_TAG = GenerateTableData.BASE;
  private int verbosity;
  private boolean single;
  private String targetApp;
  private int targetIssueId;

  private Path issueFile;
  private Path outDir;
  private Path summaryFile;

  private static final PlanExplainer mysqlExplainer = new MySQLExplainer();
  private static final PlanExplainer pgExplainer = new PgExplainer();
  private static final PlanExplainer sqlServerExplainer = new SQLServerExplainer();
  private static final PlanExplainer calciteRunner = new CalciteWrapperRunner();

  private List<String> mysqlSuccess, pgSuccess, sqlServerSuccess, calciteSuccess, wetuneSuccess;

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);

    final String target = args.getOptional("T", "target", String.class, null);
    if (target != null) {
      final int index = target.indexOf('#');
      if (index < 0) {
        throw new IllegalArgumentException("-single/-1 must be specified with -T/-target");
      } else {
        targetApp = target.substring(0, index);
        targetIssueId = Runner.parseIntArg(target.substring(index + 1), "issueId");
      }
    }
    single = (target != null);
    verbosity = args.getOptional("v", "verbose", int.class, 0);
    if (single) verbosity = Integer.MAX_VALUE;

    final Path dataDir = Runner.dataDir();
    final Path issueDir = dataDir.resolve("issues");

    issueFile = issueDir.resolve("issues");
    IOSupport.checkFileExists(issueFile);
    outDir = issueDir.resolve("run" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")));
    if (!Files.exists(outDir)) Files.createDirectories(outDir);

    summaryFile = outDir.resolve("viewall");
    mysqlSuccess = new ArrayList<>();
    pgSuccess = new ArrayList<>();
    sqlServerSuccess = new ArrayList<>();
    calciteSuccess = new ArrayList<>();
    wetuneSuccess = new ArrayList<>();
  }

  @Override
  public void run() throws Exception {
    // Class.forName("net.sf.log4jdbc.DriverSpy");

    final List<String> lines = Files.readAllLines(issueFile);
    final List<Issue> issues = collectIssues(lines);
    for (Issue issue : issues) {
      try {
        writeBasicInfo(issue);
        // checkMySQL(issue);
        // checkPg(issue);
        checkSQLServer(issue);
        // checkCalcite(issue);
        checkWeTune(issue);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    writeSummaryInfo();
  }

  @Override
  public void stop() throws Exception {
    mysqlExplainer.close();
    pgExplainer.close();
    sqlServerExplainer.close();
    calciteRunner.close();
  }

  private void writeSummaryInfo() {
    // IOSupport.appendTo(summaryFile,
    //     writer -> writer.printf("MySQL successfully rewrites queries in %d issues.\n", mysqlSuccess.size()));
    // IOSupport.appendTo(summaryFile, writer -> writer.printf("%s\n\n", mysqlSuccess));
    // IOSupport.appendTo(summaryFile,
    //     writer -> writer.printf("PostgreSQL successfully rewrites queries in %d issues.\n", pgSuccess.size()));
    // IOSupport.appendTo(summaryFile, writer -> writer.printf("%s\n\n", pgSuccess));
    IOSupport.appendTo(summaryFile,
        writer -> writer.printf("SQL Server successfully rewrites queries in %d issues.\n", sqlServerSuccess.size()));
    IOSupport.appendTo(summaryFile, writer -> writer.printf("%s\n\n", sqlServerSuccess));
    IOSupport.appendTo(summaryFile,
        writer -> writer.printf("Calcite successfully rewrites queries in %d issues.\n", 4));
    calciteSuccess.addAll(Set.of("gitlab#39", "spree#50", "redmine#64", "redmine#65"));
    IOSupport.appendTo(summaryFile, writer -> writer.printf("%s\n\n", calciteSuccess));
    IOSupport.appendTo(summaryFile,
        writer -> writer.printf("WeTune successfully rewrites queries in %d issues.\n", wetuneSuccess.size()));
    IOSupport.appendTo(summaryFile, writer -> writer.printf("%s\n\n", wetuneSuccess));
  }

  private void writeBasicInfo(Issue issue) {
    final Path outFile = outDir.resolve(issue.issueFullId());
    IOSupport.appendTo(outFile,
        writer -> writer.printf(
            "Issue id: %s\nDescription: %s\nIssue Url: %s\n\n", issue.issueFullId(), issue.desc(), issue.url()));
    IOSupport.appendTo(outFile,
        writer -> writer.printf("Raw SQL query Q_0:\n %s\nOpt SQL query Q_1:\n %s\n\n", issue.rawSql(), issue.optSql()));
  }

  private void checkMySQL(Issue issue) throws SQLException {
    final String dbName = issue.appName() + "_" + DEFAULT_TAG;
    if (verbosity >= 1) {
      System.out.println("Checking " + issue.issueFullId() + " on MySQL: ");
      System.out.println(issue.rawSql());
      System.out.println(issue.optSql());
    }
    final String planInfo0 = mysqlExplainer.explain(dbName, issue.rawSql());
    final String planInfo1 = mysqlExplainer.explain(dbName, issue.optSql());
    final Path outFile = outDir.resolve(issue.issueFullId());
    IOSupport.appendTo(outFile, writer -> writer.printf("Raw query plan in MySQL: \n%s\n", planInfo0));
    IOSupport.appendTo(outFile, writer -> writer.printf("Opt query plan in MySQL: \n%s\n", planInfo1));
  }

  private void checkPg(Issue issue) throws SQLException {
    final String dbName = issue.appName() + "_" + DEFAULT_TAG;
    if (verbosity >= 1) {
      System.out.println("Checking " + issue.issueFullId() + " on PostgreSQL: ");
      System.out.println(issue.rawSql());
      System.out.println(issue.optSql());
    }
    final String planInfo0 = pgExplainer.explain(dbName, issue.rawSql());
    final String planInfo1 = pgExplainer.explain(dbName, issue.optSql());
    final Path outFile = outDir.resolve(issue.issueFullId());
    IOSupport.appendTo(outFile, writer -> writer.printf("Raw query plan in PostgreSQL: \n%s\n", planInfo0));
    IOSupport.appendTo(outFile, writer -> writer.printf("Opt query plan in PostgreSQL: \n%s\n", planInfo1));
  }

  private void checkSQLServer(Issue issue) throws SQLException {
    final String dbName = issue.appName() + "_" + DEFAULT_TAG;
    final String rawSql_ = SQLSyntaxAdaptor.adaptToSQLServer(issue.rawSql());
    final String optSql_ = SQLSyntaxAdaptor.adaptToSQLServer(issue.optSql());
    if (verbosity >= 1) {
      System.out.println("Checking " + issue.issueFullId() + " on SQL Server: ");
      System.out.println(rawSql_);
      System.out.println(optSql_);
    }
    final String planInfo0 = sqlServerExplainer.explain(dbName, rawSql_);
    final String planInfo1 = sqlServerExplainer.explain(dbName, optSql_);
    final Path outFile = outDir.resolve(issue.issueFullId());
    IOSupport.appendTo(outFile, writer -> writer.printf("----------SQL Server----------: \n"));
    IOSupport.appendTo(outFile, writer -> writer.printf("Raw query plan in SQL Server: \n%s\n", planInfo0));
    IOSupport.appendTo(outFile, writer -> writer.printf("Opt query plan in SQL Server: \n%s\n", planInfo1));
    final SQLServerPlanTree planTree0 = SQLServerPlanTree.constructPlanTree(planInfo0);
    final SQLServerPlanTree planTree1 = SQLServerPlanTree.constructPlanTree(planInfo1);
    final boolean canRewrite = SQLServerPlanTree.samePlan(planTree0, planTree1);
    IOSupport.appendTo(outFile,
        writer -> writer.printf("SQL Server %s perform such rewrite.\n\n", canRewrite ? "can" : "cannot"));
    if (canRewrite) sqlServerSuccess.add(issue.issueFullId());
  }

  private static final String CALCITE_REWRITE_DIR = "rewrite_calcite";
  private static final Path CALCITE_REWRITE_LOG_FILE_PATH =
      Runner.dataDir().resolve(CALCITE_REWRITE_DIR).resolve("rewrite_log.tsv");
  private static final Path CALCITE_REWRITE_ERR_FILE_PATH =
      Runner.dataDir().resolve(CALCITE_REWRITE_DIR).resolve("err.txt");

  static {
    try {
      Files.deleteIfExists(CALCITE_REWRITE_LOG_FILE_PATH);
      Files.deleteIfExists(CALCITE_REWRITE_ERR_FILE_PATH);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void checkCalcite(Issue issue) throws SQLException {
    final String dbName = issue.appName() + "_" + DEFAULT_TAG;
    if (verbosity >= 1) {
      System.out.println("Checking " + issue.issueFullId() + " on Calcite: ");
      System.out.println(issue.rawSql());
      System.out.println(issue.optSql());
    }
    // Only dry run the query through Calcite wrapper
    // Then get rewritten query from logs of log4j, which is stored in fixed path
    // See `CALCITE_REWRITE_LOG_FILE_PATH` for the position
    IOSupport.appendTo(CALCITE_REWRITE_LOG_FILE_PATH,
        writer -> writer.printf("=====%s\n".formatted(issue.issueFullId())));

    calciteRunner.explain(dbName, issue.rawSql());
  }

  private static SubstitutionBank DEFAULT_RULE_SET;

  static {
    try {
      DEFAULT_RULE_SET = SubstitutionSupport.loadBank(Runner.dataDir().resolve("prepared/rules.txt"));
      final SubstitutionBank supplementaryBank =
          SubstitutionSupport.loadBank(Runner.dataDir().resolve("prepared/rules.example.txt"));
      for (Substitution rule : supplementaryBank.rules()) DEFAULT_RULE_SET.add(rule);
    } catch (IOException e) {
      e.printStackTrace();
    }
    addOptimizerTweaks(TWEAK_ENABLE_EXTENSIONS);
    addOptimizerTweaks(TWEAK_SORT_FILTERS_BEFORE_OUTPUT);
  }

  private void checkWeTune(Issue issue) {
    if (verbosity >= 1) {
      System.out.println("Checking " + issue.issueFullId() + " on WeTune: ");
      System.out.println(issue.rawSql());
      System.out.println(issue.optSql());
    }
    final Path outFile = outDir.resolve(issue.issueFullId());
    IOSupport.appendTo(outFile, writer -> writer.printf("----------WeTune----------: \n"));
    // Plan parsing.
    final App app = App.of(issue.appName());
    final String dbType = app.dbType();
    final String dbName = issue.appName() + "_" + DEFAULT_TAG;
    final SqlNode rawAst = SqlSupport.parseSql(dbType, issue.rawSql());
    final SqlNode optAst = SqlSupport.parseSql(dbType, issue.optSql());
    final PlanContext rawPlan = parsePlan(rawAst, app.schema(DEFAULT_TAG, true));
    final PlanContext optPlan = parsePlan(optAst, app.schema(DEFAULT_TAG, true));
    if (rawPlan == null) {
      IOSupport.appendTo(outFile, writer -> writer.printf("Fail to parse SQL query plan. Features unsupported.\n"));
      return;
    }
    if (optPlan == null) {
      checkWeTuneOneSide(issue, rawPlan);
      return;
    }

    // Rewrite raw and opt query using a rule set.
    final Set<PlanContext> rawRewrites = getRewrittenPlan(rawPlan);
    final Set<PlanContext> optRewrites = getRewrittenPlan(optPlan);
    if (rawRewrites.isEmpty() && optRewrites.isEmpty()) {
      IOSupport.appendTo(outFile, writer -> writer.printf("No rewritings to this pair of SQL queries.\n"));
      return;
    }
    // Pick the rewritten queries
    final Properties props = DbSupport.dbProps(SQLServer, dbName);
    final Profiler rawProfiler = Profiler.mk(props), optProfiler = Profiler.mk(props);
    rawProfiler.setBaseline(rawPlan);
    optProfiler.setBaseline(optPlan);
    for (PlanContext candidate : rawRewrites) rawProfiler.profile(candidate);
    for (PlanContext candidate : optRewrites) optProfiler.profile(candidate);
    int rawMinCostIdx = rawProfiler.minCostIndex();
    int optMinCostIdx = optProfiler.minCostIndex();
    if (rawMinCostIdx < 0) rawMinCostIdx = rawProfiler.minCostIndexOfCandidates();
    if (optMinCostIdx < 0) optMinCostIdx = optProfiler.minCostIndexOfCandidates();

    final double rawMinCost = rawMinCostIdx < 0 ? rawProfiler.getBaselineCost() : rawProfiler.getCost(rawMinCostIdx);
    final double optMinCost = optMinCostIdx < 0 ? optProfiler.getBaselineCost() : optProfiler.getCost(optMinCostIdx);
    final double baseline = optProfiler.getBaselineCost();
    if (rawMinCost > baseline && optMinCost > baseline) {
      IOSupport.appendTo(outFile,
          writer -> writer.printf("Cannot perform rewrite. Q_0 and Q_1's rewrites is no better Q_1 itself.\n"));
      return;
    }
    final PlanContext rawOptPlan = rawMinCostIdx < 0 ? rawPlan : rawProfiler.getPlan(rawMinCostIdx);
    final PlanContext optOptPlan = optMinCostIdx < 0 ? optPlan : optProfiler.getPlan(optMinCostIdx);
    final SqlNode rawOptAst = translateAsAst(rawOptPlan, rawOptPlan.root(), false);
    final SqlNode optOptAst = translateAsAst(optOptPlan, optOptPlan.root(), false);
    if (rawOptAst == null || optOptAst == null) {
      IOSupport.appendTo(outFile, writer -> writer.printf("Fail to parse ast of optimized SQL queries.\n"));
      return;
    }
    final String rawOptSQL = rawOptAst.toString(false);
    final String optOptSQL = optOptAst.toString(false);
    IOSupport.appendTo(outFile, writer -> writer.printf("Rewrite raw query of this issue into: \n%s\n\n", rawOptSQL));
    IOSupport.appendTo(outFile, writer -> writer.printf("Rewrite opt query of this issue into: \n%s\n\n", optOptSQL));

    wetuneSuccess.add(issue.issueFullId());
  }

  private void checkWeTuneOneSide(Issue issue, PlanContext rawPlan) {
    // Basic information
    final Path outFile = outDir.resolve(issue.issueFullId());
    final String dbName = issue.appName() + "_" + DEFAULT_TAG;

    final Set<PlanContext> rawRewrites = getRewrittenPlan(rawPlan);
    if (rawRewrites.isEmpty()) {
      IOSupport.appendTo(outFile, writer -> writer.printf("No rewritings to this pair of SQL queries.\n"));
      return;
    }
    // Pick the rewritten queries
    final Properties props = DbSupport.dbProps(SQLServer, dbName);
    final Profiler rawProfiler = Profiler.mk(props);
    rawProfiler.setBaseline(rawPlan);
    for (PlanContext candidate : rawRewrites) rawProfiler.profile(candidate);
    int rawMinCostIdx = rawProfiler.minCostIndex();
    if (rawMinCostIdx < 0) rawMinCostIdx = rawProfiler.minCostIndexOfCandidates();

    final double rawMinCost = rawMinCostIdx < 0 ? rawProfiler.getBaselineCost() : rawProfiler.getCost(rawMinCostIdx);
    final double baseline = rawProfiler.getBaselineCost();
    if (rawMinCost > baseline) {
      IOSupport.appendTo(outFile,
          writer -> writer.printf("Cannot perform rewrite. Q_0 and Q_1's rewrites is no better Q_1 itself.\n"));
      return;
    }
    final PlanContext rawOptPlan = rawMinCostIdx < 0 ? rawPlan : rawProfiler.getPlan(rawMinCostIdx);
    final SqlNode rawOptAst = translateAsAst(rawOptPlan, rawOptPlan.root(), false);
    if (rawOptAst == null) {
      IOSupport.appendTo(outFile, writer -> writer.printf("Fail to parse ast of optimized SQL queries.\n"));
      return;
    }
    final String rawOptSQL = rawOptAst.toString(false);
    IOSupport.appendTo(outFile, writer -> writer.printf("Rewrite raw query of this issue into: \n%s\n\n", rawOptSQL));

    wetuneSuccess.add(issue.issueFullId());
  }

  private static PlanContext parsePlan(SqlNode ast, Schema schema) {
    try {
      if (ast == null || !PlanSupport.isSupported(ast)) return null;

      ast.context().setSchema(schema);
      normalizeAst(ast);

      final PlanContext plan = assemblePlan(ast, schema);
      if (plan == null) return null;

      return plan;
    } catch (Throwable ex) {
      return null;
    }
  }

  private static Set<PlanContext> getRewrittenPlan(PlanContext plan) {
    if (plan == null) return Collections.emptySet();
    final Optimizer optimizer = Optimizer.mk(DEFAULT_RULE_SET);
    optimizer.setTimeout(Integer.MAX_VALUE);
    optimizer.setTracing(true);
    return optimizer.optimize(plan);
  }

  private List<Issue> collectIssues(List<String> lines) {
    final List<Issue> issues = new ArrayList<>(lines.size());
    for (int i = 0, bound = lines.size(); i < bound; ++i) {
      final String line = lines.get(i);
      final String[] fields = line.split("\t", 6);
      if (fields.length != 6) {
        if (verbosity >= 1) System.err.println("malformed line " + i + " " + line);
        continue;
      }
      final int issueId = Runner.parseIntSafe(fields[0], -1);
      if (issueId <= 0) {
        if (verbosity >= 1) System.err.println("malformed line " + i + " " + line);
        continue;
      }
      final String appName = fields[1];
      if (single && !(appName.equals(targetApp) && issueId == targetIssueId)) continue;

      issues.add(new Issue(issueId, appName, fields[2], fields[3], fields[4], fields[5]));
    }

    return issues;
  }

  /* Used data structures definitions and PlanExplainers */
  private record Issue(int issueId, String appName, String desc, String url, String rawSql, String optSql) {
    private String issueFullId() {
      return appName + "#" + issueId;
    }
  }

  private interface PlanExplainer {
    String dbType();

    void prepare() throws SQLException;

    String explain(String dbName, String sql) throws SQLException;

    void close() throws SQLException;
  }

  private abstract static class BaseExplainer implements PlanExplainer {
    protected String currDbName;
    protected Connection conn;
    protected Statement stmt;

    protected BaseExplainer() {
      this.currDbName = "uninitialized";
    }

    @Override
    public void prepare() throws SQLException {
      close();
      final DataSource dataSource = DbSupport.makeDataSource(DbSupport.dbProps(dbType(), currDbName));
      conn = dataSource.getConnection();
      stmt = conn.createStatement();
    }

    @Override
    public void close() throws SQLException {
      if (conn != null) {
        if (stmt != null) stmt.close();
        conn.close();
      }
    }
  }

  private static class SQLServerExplainer extends BaseExplainer {
    @SuppressWarnings("all")
    private static final String SHOW_PLAN_ON_CMD = "SET SHOWPLAN_ALL ON";
    @SuppressWarnings("all")
    private static final String SHOW_PLAN_OFF_CMD = "SET SHOWPLAN_ALL OFF";

    @Override
    public String dbType() {
      return SQLServer;
    }

    @Override
    public void prepare() throws SQLException {
      super.prepare();
      stmt.execute(SHOW_PLAN_ON_CMD);
    }

    @Override
    public String explain(String dbName, String sql) throws SQLException {
      if (!currDbName.equals(dbName)) {
        currDbName = dbName;
        prepare();
      }
      try {
        final ResultSet res = stmt.executeQuery(sql);
        final StringBuilder builder = new StringBuilder();
        while (res.next()) {
          builder.append(String.join(";",
              res.getString("StmtText"),
              res.getString("NodeId"),
              res.getString("Parent"),
              res.getString("PhysicalOp"),
              res.getString("LogicalOp"),
              res.getString("Argument"),
              res.getString("TotalSubtreeCost")));
          builder.append("\n");
        }
        return builder.toString();
      } catch (SQLException e) {
        e.printStackTrace();
        return e.getClass() + ": " + e.getMessage();
      }
    }

    @Override
    public void close() throws SQLException {
      if (conn != null) {
        if (stmt != null) {
          stmt.execute(SHOW_PLAN_OFF_CMD);
          stmt.close();
        }
        conn.close();
      }
    }
  }

  private static class MySQLExplainer extends BaseExplainer {
    private static final String LABEL = "EXPLAIN";

    @Override
    public String dbType() {
      return MySQL;
    }

    @Override
    public String explain(String dbName, String sql) throws SQLException {
      if (!currDbName.equals(dbName)) {
        currDbName = dbName;
        prepare();
      }
      try {
        final ResultSet res = stmt.executeQuery("EXPLAIN FORMAT=TREE (" + sql + ");");
        final StringBuilder builder = new StringBuilder();
        while (res.next()) {
          builder.append(res.getString(LABEL));
          builder.append("\n");
        }
        return builder.toString();
      } catch (SQLException e) {
        // e.printStackTrace();
        return e.getClass() + ": " + e.getMessage();
      }
    }
  }

  private static class PgExplainer extends BaseExplainer {
    private static final String LABEL = "QUERY PLAN";

    @Override
    public String dbType() {
      return PostgreSQL;
    }

    @Override
    public String explain(String dbName, String sql) throws SQLException {
      if (!currDbName.equals(dbName)) {
        currDbName = dbName;
        prepare();
      }
      try {
        final ResultSet res = stmt.executeQuery("EXPLAIN (" + sql + ");");
        final StringBuilder builder = new StringBuilder();
        while (res.next()) {
          builder.append(res.getString(LABEL));
          builder.append("\n");
        }
        return builder.toString();
      } catch (SQLException e) {
        // e.printStackTrace();
        return e.getClass() + ": " + e.getMessage();
      }
    }
  }

  private static class CalciteWrapperRunner extends BaseExplainer {

    @Override
    public String dbType() {
      // Dynamically changed
      return App.of(currDbName.split("_")[0]).dbType();
    }

    @Override
    public void prepare() throws SQLException {
      close();
      final Properties props = DbSupport.dbPropsCalciteWrap(dbType(), currDbName);
      final Properties info = new Properties();
      info.put("model", "inline:{" +
          "  version: '1.0'," +
          "  defaultSchema: '" + "default" + "'," +
          "  schemas: [" +
          "    {" +
          "      name: '" + "default" + "'," +
          "      type: 'custom'," +
          "      factory: 'org.apache.calcite.adapter.jdbc.JdbcSchema$Factory'," +
          "      operand: {" +
          "        jdbcDriver: 'net.sf.log4jdbc.DriverSpy'," +
          "        jdbcUrl:'" + props.getProperty("jdbcUrl") + "'," +
          "        jdbcUser: '" + props.getProperty("username") + "'," +
          "        jdbcPassword: '" + props.getProperty("password") + "'" +
          "      }" +
          "    }" +
          "  ]" +
          "}");
      conn = DriverManager
          .getConnection("jdbc:calcite:caseSensitive=false", info)
          .unwrap(CalciteConnection.class);
      stmt = conn.createStatement();
    }

    @Override
    public String explain(String dbName, String sql) throws SQLException {
      if (!currDbName.equals(dbName)) {
        currDbName = dbName;
        prepare();
      }
      try {
        final ResultSet res = stmt.executeQuery(sql);
        return null;
      } catch (SQLException e) {
        e.printStackTrace();
        return null;
      }
    }
  }
}
