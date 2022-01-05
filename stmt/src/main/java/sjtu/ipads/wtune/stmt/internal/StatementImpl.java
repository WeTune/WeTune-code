package sjtu.ipads.wtune.stmt.internal;

import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.OptStatementDao;
import sjtu.ipads.wtune.stmt.dao.StatementDao;

public class StatementImpl implements Statement {
  private final String appName;
  private final String rawSql;
  private final String stackTrace;

  private int stmtId;
  private SqlNode ast;

  private boolean isRewritten;
  private Statement otherVersion;

  protected StatementImpl(String appName, int stmtId, String rawSql, String stackTrace) {
    this.appName = appName;
    this.stmtId = stmtId;
    this.rawSql = rawSql;
    this.stackTrace = stackTrace;
  }

  public static Statement build(String appName, String rawSql, String stackTrace) {
    return build(appName, -1, rawSql, stackTrace);
  }

  public static Statement build(String appName, int stmtId, String rawSql, String stackTrace) {
    if ("broadleaf_tmp".equals(appName)) appName = "broadleaf";
    return new StatementImpl(appName, stmtId, rawSql, stackTrace);
  }

  @Override
  public String appName() {
    return appName;
  }

  @Override
  public int stmtId() {
    return stmtId;
  }

  @Override
  public String rawSql() {
    return rawSql;
  }

  @Override
  public String stackTrace() {
    return stackTrace;
  }

  @Override
  public boolean isRewritten() {
    return isRewritten;
  }

  @Override
  public SqlNode ast() {
    if (ast == null) ast = SqlSupport.parseSql(app().dbType(), rawSql);
    return ast;
  }

    @Override
  public Statement rewritten() {
    if (isRewritten) return this;
    if (otherVersion == null) otherVersion = OptStatementDao.instance().findOne(appName, stmtId);
    return otherVersion;
  }

  @Override
  public Statement original() {
    if (!isRewritten) return this;
    if (otherVersion == null) otherVersion = StatementDao.instance().findOne(appName, stmtId);
    return otherVersion;
  }

  @Override
  public void setStmtId(int stmtId) {
    this.stmtId = stmtId;
  }

  @Override
  public void setRewritten(boolean isRewritten) {
    this.isRewritten = isRewritten;
  }

  @Override
  public String toString() {
    return "%s-%d".formatted(appName(), stmtId());
  }
}
