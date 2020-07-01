package sjtu.ipads.wtune.stmt.statement;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.stmt.analyzer.Analyzer;
import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.dao.internal.StatementDaoInstance;
import sjtu.ipads.wtune.stmt.mutator.Mutator;
import sjtu.ipads.wtune.stmt.resovler.Resolver;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static sjtu.ipads.wtune.stmt.utils.StmtHelper.newInstance;

public class Statement {
  public static final String KEY_APP_NAME = "appName";
  public static final String KEY_STMT_ID = "stmtId";
  public static final String KEY_RAW_SQL = "rawSql";

  private String appName;
  private int stmtId;
  private String rawSql;

  private AppContext appContext;
  private SQLNode parsed;

  private final Set<Class<? extends Resolver>> resolvedBy = new HashSet<>();

  public static Statement findOne(String appName, int id) {
    return StatementDaoInstance.findOne(appName, id);
  }

  public static List<Statement> findByApp(String appName) {
    return StatementDaoInstance.findByApp(appName);
  }

  public static List<Statement> findAll() {
    return StatementDaoInstance.findAll();
  }

  public Statement() {
    stmtId = -1;
  }

  public String appName() {
    return appName;
  }

  public int stmtId() {
    return stmtId;
  }

  public String rawSql() {
    return rawSql;
  }

  public SQLNode parsed() {
    if (parsed == null) {
      final SQLParser parser = SQLParser.ofDb(appContext().dbType());
      if (parser != null) parsed = parser.parse(rawSql);
    }
    return parsed;
  }

  public AppContext appContext() {
    if (appContext == null) appContext = AppContext.of(appName);
    return appContext;
  }

  public void resolve(Class<? extends Resolver> cls, boolean force) {
    if (!force && resolvedBy.contains(cls)) return;

    final Resolver resolver = newInstance(cls);

    for (Class<? extends Resolver> dependency : resolver.dependsOn()) resolve(dependency, force);
    resolver.resolve(this);
    resolvedBy.add(cls);
  }

  public void resolve(Class<? extends Resolver> cls) {
    resolve(cls, false);
  }

  public void mutate(Class<? extends Mutator> cls) {
    final Mutator mutator = newInstance(cls);
    for (Class<? extends Resolver> dependency : mutator.dependsOnResolver()) resolve(dependency);
    mutator.mutate(this);
  }

  public <T> T analyze(Class<? extends Analyzer<T>> cls) {
    final Analyzer<T> analyzer = newInstance(cls);
    for (Class<? extends Resolver> dependency : analyzer.dependsOn()) resolve(dependency);
    return analyzer.analyze(this);
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public void setStmtId(int stmtId) {
    this.stmtId = stmtId;
  }

  public void setRawSql(String rawSql) {
    this.rawSql = rawSql;
    this.parsed = null;
    this.resolvedBy.clear();
  }

  public void setParsed(SQLNode parsed) {
    this.parsed = parsed;
  }

  public Statement registerToApp() {
    appContext().addStatement(this);
    return this;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Statement statement = (Statement) o;
    return stmtId == statement.stmtId && Objects.equals(appName, statement.appName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(appName, stmtId);
  }

  @Override
  public String toString() {
    return "<" + appName + ", " + stmtId + ">";
  }
}
