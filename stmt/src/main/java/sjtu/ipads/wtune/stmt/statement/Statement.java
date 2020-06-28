package sjtu.ipads.wtune.stmt.statement;

import sjtu.ipads.wtune.stmt.dao.StatementDaoInstance;

import java.util.List;
import java.util.Objects;

public class Statement {
  public static final String KEY_APP_NAME = "appName";
  public static final String KEY_STMT_ID = "stmtId";
  public static final String KEY_RAW_SQL = "rawSql";

  private String appName;
  private int stmtId;
  private String rawSql;

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

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public void setStmtId(int stmtId) {
    this.stmtId = stmtId;
  }

  public void setRawSql(String rawSql) {
    this.rawSql = rawSql;
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
