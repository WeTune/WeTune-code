package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;
import sjtu.ipads.wtune.stmt.statement.Issue;

import java.util.List;

public interface IssueDao extends Dao {
  List<Issue> findAll();

  List<Issue> findByApp(String appName);

  List<Issue> findUnchecked(String appName);

  static IssueDao instance() {
    return DaoInstances.get(IssueDao.class);
  }
}
