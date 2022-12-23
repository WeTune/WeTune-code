package wtune.testbed.runner;

import wtune.common.datasource.DbSupport;
import wtune.common.utils.Args;
import wtune.common.utils.IOSupport;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static wtune.common.datasource.DbSupport.*;

public class RewriteIssue implements Runner {
  private int verbosity;
  private boolean single;
  private String targetApp;
  private int targetIssueId;

  private Path issueFile;
  private Path outDir;

  private static final PlanExplainer mysqlExplainer = new MySQLExplainer();
  private static final PlanExplainer pgExplainer = new PgExplainer();
  private static final PlanExplainer sqlServerExplainer = new SQLServerExplainer();

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
    single = args.getOptional("1", "single", boolean.class, false);
    if (single && target == null)
      throw new IllegalArgumentException("-single/-1 must be specified with -T/-target");

    verbosity = args.getOptional("v", "verbose", int.class, 0);
    if (single) verbosity = Integer.MAX_VALUE;


    final Path dataDir = Runner.dataDir();
    final Path issueDir = dataDir.resolve("issues");

    issueFile = issueDir.resolve("issues");
    IOSupport.checkFileExists(issueFile);
    outDir = issueDir.resolve("run" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")));
    if (!Files.exists(outDir)) Files.createDirectories(outDir);

  }

  @Override
  public void run() throws Exception {
    final List<String> lines = Files.readAllLines(issueFile);
    final List<Issue> issues = collectIssues(lines);
    for (Issue issue : issues) {
      writeBasicInfo(issue);
      checkMySQL(issue);
      checkPg(issue);
      checkSQLServer(issue);
      checkWeTune(issue);
    }
    mysqlExplainer.close();
    pgExplainer.close();
    sqlServerExplainer.close();
  }

  private void writeBasicInfo(Issue issue) {
    final Path outFile = outDir.resolve(issue.issueFullId());
    IOSupport.appendTo(outFile,
        writer ->
            writer.printf("%s\nDescription: %s\nIssue Url: %s\n", issue.issueFullId(), issue.desc(), issue.url()));
  }

  private void checkMySQL(Issue issue) throws SQLException {
    final String dbName = issue.appName() + "_base";
    final String planInfo0 = mysqlExplainer.explain(dbName, issue.rawSql());
    final String planInfo1 = mysqlExplainer.explain(dbName, issue.optSql());
    final Path outFile = outDir.resolve(issue.issueFullId());
    IOSupport.appendTo(outFile, writer -> writer.printf("Raw SQL plan in MySQL: \n%s\n", planInfo0));
    IOSupport.appendTo(outFile, writer -> writer.printf("Opt SQL plan in MySQL: \n%s\n", planInfo1));
  }

  private void checkPg(Issue issue) throws SQLException {
    final String dbName = issue.appName() + "_base";
    final String planInfo0 = pgExplainer.explain(dbName, issue.rawSql());
    final String planInfo1 = pgExplainer.explain(dbName, issue.optSql());
    final Path outFile = outDir.resolve(issue.issueFullId());
    IOSupport.appendTo(outFile, writer -> writer.printf("Raw SQL plan in PostgreSQL: \n%s\n", planInfo0));
    IOSupport.appendTo(outFile, writer -> writer.printf("Opt SQL plan in PostgreSQL: \n%s\n", planInfo1));
  }

  private void checkSQLServer(Issue issue) throws SQLException {
    final String dbName = issue.appName() + "_base";
    final String planInfo0 = sqlServerExplainer.explain(dbName, issue.rawSql());
    final String planInfo1 = sqlServerExplainer.explain(dbName, issue.optSql());
    final Path outFile = outDir.resolve(issue.issueFullId());
    IOSupport.appendTo(outFile, writer -> writer.printf("Raw SQL plan in SQL Server: \n%s\n", planInfo0));
    IOSupport.appendTo(outFile, writer -> writer.printf("Opt SQL plan in SQL Server: \n%s\n", planInfo1));
  }

  private void checkWeTune(Issue issue) {
  }

  private void checkCalcite(Issue issue) {
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
      final String appName = fields[0];
      final int issueId = Runner.parseIntSafe(fields[1], -1);
      if (issueId <= 0) {
        if (verbosity >= 1) System.err.println("malformed line " + i + " " + line);
        continue;
      }
      if (single && !(appName.equals(targetApp) || issueId == targetIssueId)) continue;

      issues.add(new Issue(fields[0], issueId, fields[2], fields[3], fields[4], fields[5]));
    }

    return issues;
  }

  private record Issue(String appName, int issueId, String desc, String url, String rawSql, String optSql) {
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
      final ResultSet res = stmt.executeQuery("EXPLAIN FORMAT=TREE (" + sql + ");");
      final StringBuilder builder = new StringBuilder();
      while (res.next()) {
        builder.append(res.getString(LABEL));
        builder.append("\n");
      }
      return builder.toString();
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
      final ResultSet res = stmt.executeQuery("EXPLAIN (" + sql + ");");
      final StringBuilder builder = new StringBuilder();
      while (res.next()) {
        builder.append(res.getString(LABEL));
        builder.append("\n");
      }
      return builder.toString();
    }
  }
}
