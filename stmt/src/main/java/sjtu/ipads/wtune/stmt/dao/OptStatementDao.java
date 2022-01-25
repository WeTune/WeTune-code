package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.internal.DbOptStatementDao;
import sjtu.ipads.wtune.stmt.support.OptimizerType;

import java.util.List;

public interface OptStatementDao {
  Statement findOne(String appName, int stmtId);

  List<Statement> findByApp(String appName);

  List<Statement> findAll();

  static OptStatementDao instance(OptimizerType type) {
    return DbOptStatementDao.instance(type);
  }
}
