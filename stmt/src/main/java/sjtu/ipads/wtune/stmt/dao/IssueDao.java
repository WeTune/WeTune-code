package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DbIssueDao;
import sjtu.ipads.wtune.stmt.support.Issue;

import java.util.List;

public interface IssueDao {
  List<Issue> findAll();

  List<Issue> findByApp(String appName);

  List<Issue> findUnchecked(String appName);

  static IssueDao instance() {
    return DbIssueDao.instance();
  }
}
