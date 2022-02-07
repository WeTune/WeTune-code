package wtune.stmt.support;

import wtune.stmt.Statement;
import wtune.stmt.dao.OptStatementDao;

public interface UpdateStmts {
  static void cleanOptStmts(OptimizerType type) {
    OptStatementDao.instance(type).cleanOptStmts();
  }

  static void updateOptStmts(Statement stmt, OptimizerType type) {
    OptStatementDao.instance(type).updateOptStmts(stmt);
  }
}
