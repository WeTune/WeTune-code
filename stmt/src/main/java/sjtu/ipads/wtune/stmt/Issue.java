package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.stmt.dao.IssueDao;

import java.util.List;

public class Issue {
  private String appName;
  private int stmtId;
  private Statement stmt;

  public static List<Issue> findAll() {
    return IssueDao.instance().findAll();
  }

  public static List<Issue> findByApp(String appName) {
    return IssueDao.instance().findByApp(appName);
  }

  public static List<Issue> findUnchecked(String appName) {
    return IssueDao.instance().findUnchecked(appName);
  }

  public String appName() {
    return appName;
  }

  public int stmtId() {
    return stmtId;
  }

  public Statement stmt() {
    if (stmt == null) stmt = Statement.findOne(appName, stmtId);
    return stmt;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public void setStmtId(int stmtId) {
    this.stmtId = stmtId;
  }
}
