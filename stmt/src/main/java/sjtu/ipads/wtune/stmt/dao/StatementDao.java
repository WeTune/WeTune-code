package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.dao.internal.DaoInstances;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

public interface StatementDao extends Dao {
  Statement findOne(String appName, int stmtId);

  List<Statement> findByApp(String appName);

  List<Statement> findAll();

  void delete(Statement stmt, String cause);

  static StatementDao instance() {
    return DaoInstances.get(StatementDao.class);
  }
}
