package sjtu.ipads.wtune.stmt.statement;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.dao.internal.StatementDaoInstance;
import sjtu.ipads.wtune.stmt.resovler.Resolver;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Statement {
  public static final String KEY_APP_NAME = "appName";
  public static final String KEY_STMT_ID = "stmtId";
  public static final String KEY_RAW_SQL = "rawSql";

  private String appName;
  private int stmtId;
  private String rawSql;

  private AppContext appContext;
  private SQLNode parsed;

  private Set<Class<? extends Resolver>> resolvedBy = new HashSet<>();

  public static Statement findOne(String appName, int id) {
    final Statement stmt = StatementDaoInstance.findOne(appName, id);
    if (stmt != null) stmt.registerToApp();
    return stmt;
  }

  public static List<Statement> findByApp(String appName) {
    final List<Statement> stmts = StatementDaoInstance.findByApp(appName);
    if (stmts != null) stmts.forEach(Statement::registerToApp);
    return stmts;
  }

  public static List<Statement> findAll() {
    final List<Statement> stmts = StatementDaoInstance.findAll();
    if (stmts != null) stmts.forEach(Statement::registerToApp);
    return stmts;
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

    final Resolver resolver;

    try {
      resolver = cls.getDeclaredConstructor().newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      throw new StmtException(e);
    }

    for (Class<? extends Resolver> dependency : resolver.dependsOn()) resolve(dependency, force);

    resolver.resolve(this);
    resolvedBy.add(cls);
  }

  public void resolve(Class<? extends Resolver> cls) {
    resolve(cls, false);
  }

  private void registerToApp() {
    appContext().addStatement(this);
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public void setStmtId(int stmtId) {
    this.stmtId = stmtId;
  }

  public void setRawSql(String rawSql) {
    this.rawSql = rawSql;
  }

  public void setParsed(SQLNode parsed) {
    this.parsed = parsed;
  }

  @Override
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
    return "<" + appName + "," + stmtId + "> " + rawSql;
  }
}
