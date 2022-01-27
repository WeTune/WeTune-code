package sjtu.ipads.wtune.stmt.internal;

import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.CalciteOptStatementDao;
import sjtu.ipads.wtune.stmt.dao.CalciteStatementDao;
import sjtu.ipads.wtune.stmt.support.OptimizerType;

public class CalciteStatementImpl implements Statement {
  private final String appName;
  private final String rawSql;
  private final String stackTrace;

  private int stmtId;
  private SqlNode ast;

  private boolean isRewritten;
  private Statement otherVersion;

  private Statement calciteVersion;

  protected CalciteStatementImpl(String appName, int stmtId, String rawSql, String stackTrace) {
    this.appName = appName;
    this.stmtId = stmtId;
    this.rawSql = rawSql;
    this.stackTrace = stackTrace;
  }

  public static Statement build(String appName, String rawSql, String stackTrace) {
    return build(appName, -1, rawSql, stackTrace);
  }

  public static Statement build(String appName, int stmtId, String rawSql, String stackTrace) {
    return new CalciteStatementImpl(appName, stmtId, rawSql, stackTrace);
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
    return rewritten(OptimizerType.WeTune);
  }

  @Override
  public Statement rewritten(OptimizerType type) {
    if (isRewritten) return this;
    if (otherVersion == null)
      otherVersion = CalciteOptStatementDao.instance().findOne(appName, stmtId);
    return otherVersion;
  }

  @Override
  public Statement original() {
    if (!isRewritten) return this;
    if (otherVersion == null)
      otherVersion = CalciteStatementDao.instance().findOne(appName, stmtId);
    return otherVersion;
  }

  @Override
  public Statement calciteVersion() {
    if (calciteVersion == null)
      calciteVersion = CalciteStatementDao.instance().findOneCalciteVersion(appName, stmtId);
    return null;
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
